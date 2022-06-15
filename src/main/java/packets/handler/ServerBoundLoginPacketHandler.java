package packets.handler;

import config.Config;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

import static util.PrintUtils.devPrint;

public class ServerBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundLoginPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("login_start", provider -> {
            String username = provider.readString();

            // for 1.19, client sends public key and signature
            if (Config.versionReporter().isAtLeast1_19()) {
                boolean hasSigData = provider.readBoolean();
                if (hasSigData) {
                    provider.readLong(); // timestamp
                    getConnectionManager().getEncryptionManager().setClientProfilePublicKey(provider.readByteArray(provider.readVarInt()));
                    getConnectionManager().getEncryptionManager().setClientProfileSignature(provider.readByteArray(provider.readVarInt()));
                }
            }

            devPrint("Login by: " + username);

            getConnectionManager().getEncryptionManager().setUsername(username);
            return true;
        });

        operations.put("encryption_response", provider -> {
            int sharedSecretLength = provider.readVarInt();
            byte[] sharedSecret = provider.readByteArray(sharedSecretLength);

            boolean isNonce = true;
            if (Config.versionReporter().isAtLeast1_19()) {
                isNonce = provider.readBoolean();
            }

            if (isNonce) {
                byte[] nonce = provider.readByteArray(provider.readVarInt());
                getConnectionManager().getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, nonce);
            } else {
                // for 1.19+ we need to handle client's public key verification step
                byte[] salt = provider.readByteArray(8);
                byte[] signature = provider.readByteArray(provider.readVarInt());

                getConnectionManager().getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, salt, signature);
            }

            return false;
        });
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return false;
    }
}
