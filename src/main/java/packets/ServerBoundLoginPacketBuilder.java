package packets;

import java.util.Arrays;

public class ServerBoundLoginPacketBuilder extends PacketBuilder {
    private final static int LOGIN_START = 0x00;
    private final static int ENCRYPTION_RESPONSE = 0x01;
    private final static int LOGIN_PLUGIN_RESPONSE = 0x02;

    @Override
    public void build(int size) {
        int packetId = getReader().readVarInt();

        switch (packetId) {
            case LOGIN_START:
                String username = getReader().readString();
                System.out.println("Login by: " + username);
                break;

            case ENCRYPTION_RESPONSE:
                int sharedSecretLength = getReader().readVarInt();
                byte[] sharedSecret = getReader().readByteArray(sharedSecretLength);
                int verifyTokenLength = getReader().readVarInt();
                byte[] verifyToken = getReader().readByteArray(verifyTokenLength);
                System.out.println("Encryption confirmation: (" + sharedSecretLength + ") " + Arrays.toString(sharedSecret));
                System.out.println("\tVerify token: (" + verifyTokenLength + ") " + Arrays.toString(verifyToken));

                break;
        }
    }


}
