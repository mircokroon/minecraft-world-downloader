package packets;

import game.Coordinates;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private final int CHUNK_DATA = 0x20;

    private final int BLOCK_CHANGE = 0x0B;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case CHUNK_DATA:
                System.out.println("Received chunk data!");
                break;



            case BLOCK_CHANGE:
                Coordinates coords = typeProvider.readCoordinates();
                int blockId = typeProvider.readVarInt();
                int type = blockId >> 4;
                int meta = blockId & 15;
                System.out.println("Changed block " + coords + " :: " + type + ":" + meta);

        }

        return true;
    }
}
