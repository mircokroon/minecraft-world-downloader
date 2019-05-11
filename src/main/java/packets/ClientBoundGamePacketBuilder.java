package packets;

import game.data.Chunk;
import game.data.Coordinate3D;
import game.data.Writer;

import java.io.IOException;

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
                    Chunk.readChunkDataPacket(typeProvider);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;



            case BLOCK_CHANGE:
                Coordinate3D coords = typeProvider.readCoordinates();
                int blockId = typeProvider.readVarInt();
                int type = blockId >> 4;
                int meta = blockId & 15;

                //System.out.println("Changed block " + coords + " :: " + type + ":" + meta);
                //System.out.println("Chunk data: ");
                //Chunk.printBlockInfo(coords);
                try {
                    System.out.println("Writing chunk");
                    Writer.write(Chunk.getChunk(coords));
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        return true;
    }
}
