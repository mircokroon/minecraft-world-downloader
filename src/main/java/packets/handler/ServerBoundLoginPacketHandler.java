package packets.handler;

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

            devPrint("Login by: " + username);

            getConnectionManager().getEncryptionManager().setUsername(username);
            return true;
        });

        operations.put("encryption_response", provider -> {
            int sharedSecretLength = provider.readVarInt();
            byte[] sharedSecret = provider.readByteArray(sharedSecretLength);
            int verifyTokenLength = provider.readVarInt();
            byte[] verifyToken = provider.readByteArray(verifyTokenLength);

            getConnectionManager().getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, verifyToken);
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
