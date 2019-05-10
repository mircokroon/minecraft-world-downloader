package packets;

import game.Game;
import game.NetworkMode;

import java.util.Arrays;

public class ClientBoundLoginPacketBuilder extends PacketBuilder {
    public final static int DISCONNECT = 0x00;
    public final static int ENCRYPTION_REQUEST = 0x01;
    public final static int LOGIN_SUCCESS = 0x02;
    public final static int SET_COMPRESSION = 0x03;

    @Override
    public boolean build(int size) {
        int packetID = getReader().readVarInt();

        switch (packetID) {
            case DISCONNECT:
                String reason = getReader().readString();
                System.out.println("Disconnect: " + reason);
                return true;
            case ENCRYPTION_REQUEST:
                String serverId = getReader().readString();
                int pubKeyLen = getReader().readVarInt();
                byte[] pubKey = getReader().readByteArray(pubKeyLen);
                int verifyTokenLen = getReader().readVarInt();
                byte[] verifyToken = getReader().readByteArray(verifyTokenLen);

                System.out.println("Received encryption request! Key: (" + pubKeyLen + ") " + Arrays.toString(pubKey));
                System.out.println("\tVerify token: (" + verifyTokenLen + ") " + Arrays.toString(verifyToken));

                Game.getEncryptionManager().setServerEncryptionRequest(pubKey, verifyToken, serverId);
                return false;
            case LOGIN_SUCCESS:
                String uuid = getReader().readString();
                String username = getReader().readString();
                System.out.println("Login success: " + username + " logged in with uuid " + uuid);
                Game.setMode(NetworkMode.GAME);
                return true;

            case SET_COMPRESSION:
                int limit = getReader().readVarInt();
                System.out.println("Compression set with limit: " + limit);
                Game.getEncryptionManager().setCompressionLimit(limit);
                return true;
            default:
                return super.build(size);
        }
    }
}
