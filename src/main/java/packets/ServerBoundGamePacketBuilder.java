package packets;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;

public class ServerBoundGamePacketBuilder extends PacketBuilder {
    private final int BLOCK_PLACE = 0x1F;
    private final int PLAYER_POSITION = 0x0D;
    private final int PLAYER_POSITION_LOOK = 0x0E;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case BLOCK_PLACE:
                Coordinate3D coordinates = typeProvider.readCoordinates();
                int face = typeProvider.readVarInt();
                int hand = typeProvider.readVarInt();
                System.out.println("Player placed a block at " + coordinates + " (face: " + face + ", hand: " + hand + ")");
                break;
            case PLAYER_POSITION:
            case PLAYER_POSITION_LOOK:
                double x = typeProvider.readDouble();
                double y = typeProvider.readDouble();
                double z = typeProvider.readDouble();
                Game.setPlayerPosition(new Coordinate3D(x, y, z).offset());
                break;
        }
        return true;// super.build(size);
    }
}

