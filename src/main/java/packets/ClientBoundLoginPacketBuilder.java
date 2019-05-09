package packets;

import java.util.Arrays;

public class ClientBoundLoginPacketBuilder extends PacketBuilder {
    private final static int DISCONNECT = 0x00;
    private final static int ENCRYPTION_REQUEST = 0x01;
    private final static int LOGIN_SUCCESS = 0x02;
    private final static int SET_COMPRESSION = 0x03;

    @Override
    public void build(int size) {
        int packetID = getReader().readVarInt();

        switch (packetID) {
            case DISCONNECT:
                String reason = getReader().readString();
                System.out.println("Disconnect: " + reason);
                break;
            case ENCRYPTION_REQUEST:
                String serverId = getReader().readString();
                int pubKeyLen = getReader().readVarInt();
                byte[] pubKey = getReader().readByteArray(pubKeyLen);
                int verifyTokenLen = getReader().readVarInt();
                byte[] verifyToken = getReader().readByteArray(verifyTokenLen);

                System.out.println("Received encryption request! Key: (" + pubKeyLen + ") " + Arrays.toString(pubKey));
                System.out.println("\tVerify token: (" + verifyTokenLen + ") " + Arrays.toString(verifyToken));
                break;
            case LOGIN_SUCCESS:
                String uuid = getReader().readString();
                String username = getReader().readString();
                System.out.println("Login success: " + username + " logged in with uuid " + uuid);
                break;
            default:
                super.build(size);
        }
    }
}
