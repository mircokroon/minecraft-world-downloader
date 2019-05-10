package packets;

import com.sun.deploy.security.ValidationState;
import game.Game;

import java.util.Arrays;

public class ServerBoundLoginPacketBuilder extends PacketBuilder {
    public final static int LOGIN_START = 0x00;
    public final static int ENCRYPTION_RESPONSE = 0x01;
    public final static int LOGIN_PLUGIN_RESPONSE = 0x02;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);

        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case LOGIN_START:
                String username = typeProvider.readString();
                System.out.println("Login by: " + username);
                return true;

            case ENCRYPTION_RESPONSE:
                int sharedSecretLength = typeProvider.readVarInt();
                byte[] sharedSecret = typeProvider.readByteArray(sharedSecretLength);
                int verifyTokenLength = typeProvider.readVarInt();
                byte[] verifyToken = typeProvider.readByteArray(verifyTokenLength);
                System.out.println("Encryption confirmation: (" + sharedSecretLength + ") " + Arrays.toString(
                    sharedSecret));
                System.out.println("\tVerify token: (" + verifyTokenLength + ") " + Arrays.toString(verifyToken));


                Game.getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, verifyToken);
                return false;
            default:
                return super.build(size);
        }
    }


}
