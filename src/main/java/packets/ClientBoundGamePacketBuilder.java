package packets;

import game.data.Chunk;
import game.data.Coordinate3D;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private final int CHUNK_DATA = 0x20;

    private final int BLOCK_CHANGE = 0x0B;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case CHUNK_DATA:
                Chunk.readChunkDataPacket(typeProvider);
                break;



            case BLOCK_CHANGE:
                Coordinate3D coords = typeProvider.readCoordinates();
                int blockId = typeProvider.readVarInt();
                int type = blockId >> 4;
                int meta = blockId & 15;

                //System.out.println("Changed block " + coords + " :: " + type + ":" + meta);
                //System.out.println("Chunk data: ");
                //Chunk.printBlockInfo(coords);
                System.out.println(Chunk.existingChunks);

        }

        return true;
    }
}
