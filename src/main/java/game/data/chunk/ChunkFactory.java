package game.data.chunk;

import game.Game;
import game.data.Coordinate2D;
import game.data.WorldManager;
import game.data.chunk.version.Chunk_1_12;
import game.data.chunk.version.Chunk_1_13;
import game.data.chunk.version.Chunk_1_14;
import gui.GuiManager;
import packets.DataTypeProvider;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkFactory extends Thread {
    private static ChunkFactory factory;

    private ConcurrentLinkedQueue<DataTypeProvider> unparsedChunks;

    private ChunkFactory() {
        this.unparsedChunks = new ConcurrentLinkedQueue<>();
    }

    public static void addChunk(DataTypeProvider provider) {
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
                    ex.printStackTrace();
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

    /**
     * Gets an unparsed chunk from the list, or null if the list is empty.
     */
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
            chunk = getVersionedChunk(chunkPos);
        } else {
            chunk = WorldManager.getChunk(new Coordinate2D(chunkPos.getX(), chunkPos.getZ()));
            chunk.setSaved(false);
        }

        // TODO: move the below into the Chunk class
        int mask = dataProvider.readVarInt();
        int size = dataProvider.readVarInt();
        chunk.readChunkColumn(full, mask, dataProvider);

        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            chunk.addTileEntity(dataProvider.readCompoundTag());
        }
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(Coordinate2D chunkPos) {
        if (Game.getProtocolVersion() >= 440) {
            return new Chunk_1_14(chunkPos.getX(), chunkPos.getZ());
        } else if (Game.getProtocolVersion() >= 341) {
            return new Chunk_1_13(chunkPos.getX(), chunkPos.getZ());
        } else {
            return new Chunk_1_12(chunkPos.getX(), chunkPos.getZ());
        }
    }
}
