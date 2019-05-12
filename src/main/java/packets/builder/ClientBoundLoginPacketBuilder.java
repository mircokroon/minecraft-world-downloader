package packets.builder;

import game.Game;
import game.NetworkMode;
import packets.DataTypeProvider;

public class ClientBoundLoginPacketBuilder extends PacketBuilder {
    public final static int DISCONNECT = 0x00;
    public final static int ENCRYPTION_REQUEST = 0x01;
    public final static int LOGIN_SUCCESS = 0x02;
    public final static int SET_COMPRESSION = 0x03;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetID = typeProvider.readVarInt();

        switch (packetID) {
            case DISCONNECT:
                String reason = typeProvider.readString();
                System.out.println("Disconnect: " + reason);
                return true;
            case ENCRYPTION_REQUEST:
                String serverId = typeProvider.readString();
                int pubKeyLen = typeProvider.readVarInt();
                byte[] pubKey = typeProvider.readByteArray(pubKeyLen);
                int verifyTokenLen = typeProvider.readVarInt();
                byte[] verifyToken = typeProvider.readByteArray(verifyTokenLen);

                Game.getEncryptionManager().setServerEncryptionRequest(pubKey, verifyToken, serverId);
                return false;
            case LOGIN_SUCCESS:
                String uuid = typeProvider.readString();
                String username = typeProvider.readString();
                System.out.println("Login success: " + username + " logged in with uuid " + uuid);
                Game.setMode(NetworkMode.GAME);
                return true;

            case SET_COMPRESSION:
                int limit = typeProvider.readVarInt();
                Game.getCompressionManager().enableCompression(limit);
                return true;
            default:
                return true;
        }
    }
}
