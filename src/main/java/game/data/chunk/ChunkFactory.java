package game.data.chunk;

import game.data.Coordinate2D;
import game.data.WorldManager;
import gui.GuiManager;
import packets.DataTypeProvider;

import java.util.LinkedList;
import java.util.Queue;

public class ChunkFactory extends Thread {
    private static ChunkFactory factory;

    private Queue<DataTypeProvider> unparsedChunks;

    private ChunkFactory() {
        this.unparsedChunks = new LinkedList<>();
    }

    public static synchronized void addChunk(DataTypeProvider provider) {
        factory.addChunkToQueue(provider);
    }

    /**
     * Need a non-static method to do this as we cannot otherwise call notify
     */
    public synchronized void addChunkToQueue(DataTypeProvider provider) {
        unparsedChunks.add(provider);
        notify();
    }

    /**
     * Start service to periodically parse chunks in the queue. This is to prevent the other threads from being blocked
     * by chunk parsing.
     */
    public static void startChunkParserService() {
        if (factory != null) {
            return;
        }

        factory = new ChunkFactory();
        factory.start();
    }

    /**
     * Periodically check if there are unparsed chunks, and if so, parse them.
     */
    @Override
    public synchronized void run() {
        DataTypeProvider provider;
        while (true) {
            while ((provider = getUnparsedChunk()) != null) {
                try {
                    readChunkDataPacket(provider);
                } catch (Exception ex) {
                    System.out.println("Chunk could not be parsed!");
                }
            }

            try {
                wait();
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

    /**
     * Parse a chunk data packet. Largely based on: https://wiki.vg/Protocol
     */
    private static void readChunkDataPacket(DataTypeProvider dataProvider) {
        Coordinate2D chunkPos = new Coordinate2D(dataProvider.readInt(), dataProvider.readInt());
        chunkPos.offsetChunk();
        GuiManager.setChunkLoaded(chunkPos);

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = new Chunk(chunkPos.getX(), chunkPos.getZ());
        } else {
            chunk = WorldManager.getChunk(new Coordinate2D(chunkPos.getX(), chunkPos.getZ()));
            chunk.setSaved(false);
        }
        int mask = dataProvider.readVarInt();
        int size = dataProvider.readVarInt();
        chunk.readChunkColumn(full, mask, dataProvider);

        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            chunk.addTileEntity(dataProvider.readCompoundTag());
        }
    }
}
