package packets.handler;

import config.Config;
import config.Option;
import config.Version;
import config.VersionReporter;
import game.NetworkMode;
import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundLoginPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("LoginDisconnect", provider -> {
            String reason = provider.readString();
            System.out.println("Disconnect: " + reason);
            return true;
        });
        operations.put("Hello", provider -> {
            String serverId = provider.readString();
            byte[] pubKey = provider.readByteArray(provider.readVarInt());
            byte[] nonce = provider.readByteArray(provider.readVarInt());

            if (Config.versionReporter().isAtLeast(Version.V1_20_6)) {
                boolean shouldAuthenticate = provider.readBoolean();
                getConnectionManager().getEncryptionManager().setServerEncryptionRequest(pubKey, nonce, serverId, shouldAuthenticate);
            } else {
                getConnectionManager().getEncryptionManager().setServerEncryptionRequest(pubKey, nonce, serverId);
            }

            return false;
        });
        operations.put("GameProfile", provider -> {
            String uuid = Config.versionReporter().select(String.class,
                    Option.of(Version.V1_16, () -> provider.readUUID().toString()),
                    Option.of(Version.ANY, provider::readString)
            );

            String username = provider.readString();
            System.out.println("Login success: " + username + " logged in with uuid " + uuid);

            return true;
        });
        operations.put("LoginCompression", provider -> {
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
