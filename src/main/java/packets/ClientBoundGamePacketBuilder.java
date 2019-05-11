package packets;

import game.data.Coordinate3D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private final int CHUNK_DATA = 0x20;

    private final int BLOCK_CHANGE = 0x0B;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case CHUNK_DATA:
                try {
                    ChunkFactory.addChunk(typeProvider);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;

            case BLOCK_CHANGE:
                Coordinate3D coords = typeProvider.readCoordinates();
                int blockId = typeProvider.readVarInt();
                int type = blockId >> 4;
                int meta = blockId & 15;

        }

        return true;
    }
}
