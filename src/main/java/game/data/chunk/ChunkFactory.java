package game.data.chunk;

import game.data.Coordinate2D;
import game.data.WorldManager;
import packets.DataTypeProvider;

import java.util.LinkedList;
import java.util.Queue;

public class ChunkFactory extends Thread {
    private static int CHUNK_PARSER_SLEEP = 1000;
    private static ChunkFactory factory;

    private Queue<DataTypeProvider> unparsedChunks;

    private ChunkFactory() {
        this.unparsedChunks = new LinkedList<>();
    }

    public static synchronized void addChunk(DataTypeProvider provider) {
        factory.unparsedChunks.add(provider);
    }

    public static void startChunkParserService() {
        if (factory != null) {
            return;
        }

        factory = new ChunkFactory();
        factory.start();
    }

    @Override
    public void run() {
        DataTypeProvider provider;
        while (true) {
            while ((provider = getUnparsedChunk()) != null ) {
                readChunkDataPacket(provider);
            }

            try {
                Thread.sleep(CHUNK_PARSER_SLEEP);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized DataTypeProvider getUnparsedChunk() {
        if (unparsedChunks.isEmpty()) {
             return null;
        }
        return unparsedChunks.remove();
    }

    private static Chunk readChunkDataPacket(DataTypeProvider dataProvider) {
        int x = dataProvider.readInt();
        int z = dataProvider.readInt();

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = new Chunk(x, z);
        } else {
            chunk = WorldManager.getChunk(new Coordinate2D(x, z));
            chunk.setSaved(false);
        }
        int mask = dataProvider.readVarInt();
        int size = dataProvider.readVarInt();
        chunk.readChunkColumn(full, mask, dataProvider);

        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            chunk.addTileEntity(dataProvider.readCompoundTag());
        }

        return chunk;
    }
}
