package packets.handler;

import static util.PrintUtils.devPrint;

import config.Config;
import config.Version;
import game.NetworkMode;
import java.util.HashMap;
import java.util.Map;
import proxy.ConnectionManager;

public class ServerBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundLoginPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("Hello", provider -> {
            String username = provider.readString();

            devPrint("Login by: " + username);

            getConnectionManager().getEncryptionManager().setUsername(username);
            return true;
        });

        operations.put("Key", provider -> {
            int sharedSecretLength = provider.readVarInt();
            byte[] sharedSecret = provider.readByteArray(sharedSecretLength);
            byte[] nonce = provider.readByteArray(provider.readVarInt());
            getConnectionManager().getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, nonce);

            return false;
        });

        operations.put("LoginAcknowledged", provider -> {
            if (Config.versionReporter().isAtLeast(Version.V1_20_2)) {
                getConnectionManager().setMode(NetworkMode.CONFIGURATION);
            } else {
                getConnectionManager().setMode(NetworkMode.GAME);
            }
            return true;
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
