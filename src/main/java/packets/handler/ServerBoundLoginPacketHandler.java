package packets.handler;

import static util.PrintUtils.devPrint;

import java.util.HashMap;
import java.util.Map;
import proxy.ConnectionManager;

public class ServerBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundLoginPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("login_start", provider -> {
            String username = provider.readString();

            devPrint("Login by: " + username);

            getConnectionManager().getEncryptionManager().setUsername(username);
            return true;
        });

        operations.put("encryption_response", provider -> {
            int sharedSecretLength = provider.readVarInt();
            byte[] sharedSecret = provider.readByteArray(sharedSecretLength);
            byte[] nonce = provider.readByteArray(provider.readVarInt());
            getConnectionManager().getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, nonce);

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
