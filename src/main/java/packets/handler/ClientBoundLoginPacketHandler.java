package packets.handler;

import game.Config;
import game.NetworkMode;
import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundLoginPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("disconnect", provider -> {
            String reason = provider.readString();
            System.out.println("Disconnect: " + reason);
            return true;
        });
        operations.put("encryption_request", provider -> {
            String serverId = provider.readString();
            int pubKeyLen = provider.readVarInt();
            byte[] pubKey = provider.readByteArray(pubKeyLen);
            int verifyTokenLen = provider.readVarInt();
            byte[] verifyToken = provider.readByteArray(verifyTokenLen);

            getConnectionManager().getEncryptionManager().setServerEncryptionRequest(pubKey, verifyToken, serverId);
            return false;
        });
        operations.put("login_success", provider -> {
            String uuid;
            if (Config.getProtocolVersion() >= 735) {
                uuid = provider.readUUID().toString();
            } else {
                // pre 1.16
                uuid = provider.readString();
            }
            String username = provider.readString();
            System.out.println("Login success: " + username + " logged in with uuid " + uuid);
            getConnectionManager().setMode(NetworkMode.GAME);
            return true;
        });
        operations.put("set_compression", provider -> {
            int limit = provider.readVarInt();
            getConnectionManager().getCompressionManager().enableCompression(limit);
            return true;
        });

    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
