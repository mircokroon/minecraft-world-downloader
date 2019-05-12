package packets;

import game.data.Coordinate3D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private final int CHUNK_DATA = 0x20;

    private final int PLAYER_POSITION = 0x32;

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
            case PLAYER_POSITION:
                /*double x = typeProvider.readDouble();
                double y = typeProvider.readDouble();
                double z = typeProvider.readDouble();
                System.out.println("Player pos: " + (int) x + ", " + (int) z);*/
                break;

        }

        return true;
    }
}
