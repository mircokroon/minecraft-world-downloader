package packets;

import game.Coordinate3D;

public class ServerBoundGamePacketBuilder extends PacketBuilder {
    private final int BLOCK_PLACE = 0x1F;

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
        }
        return true;// super.build(size);
    }
}

