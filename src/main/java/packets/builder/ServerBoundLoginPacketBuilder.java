package packets.builder;

import game.Game;
import packets.DataTypeProvider;

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

                Game.getEncryptionManager().setUsername(username);
                return true;

            case ENCRYPTION_RESPONSE:
                int sharedSecretLength = typeProvider.readVarInt();
                byte[] sharedSecret = typeProvider.readByteArray(sharedSecretLength);
                int verifyTokenLength = typeProvider.readVarInt();
                byte[] verifyToken = typeProvider.readByteArray(verifyTokenLength);

                Game.getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, verifyToken);
                return false;
            default:
                return true;
        }
    }
}
