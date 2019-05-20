package game.data.chunk;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.version.Chunk_1_12;
import game.data.chunk.version.Chunk_1_13;
import game.data.chunk.version.Chunk_1_14;
import gui.GuiManager;
import javafx.util.Pair;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Class responsible for creating chunks.
 */
public class ChunkFactory extends Thread {
    private static ChunkFactory factory;

    private ConcurrentLinkedQueue<DataTypeProvider> unparsedChunks;
    private ConcurrentMap<Coordinate2D,ConcurrentLinkedQueue<TileEntity>> tileEntities;

    public static ChunkFactory getInstance() {
        if (factory == null) {
            factory = new ChunkFactory();
        }
        return factory;
    }

    private ChunkFactory() {
        this.tileEntities = new ConcurrentHashMap<>();
        this.unparsedChunks = new ConcurrentLinkedQueue<>();
    }

    /**
     * Update a tile entity that was given individually.
     * @param position the uncorrected position of the tile entity
     * @param entityData the NBT data of the entity
     */
    public void updateTileEntity(Coordinate3D position, SpecificTag entityData) {
        position.offset();

        Chunk chunk = WorldManager.getChunk(position.chunkPos());

        // if the chunk doesn't exist yet, add it to the queue to process later
        if (chunk == null) {
            Queue<TileEntity> queue = tileEntities
                .computeIfAbsent(position.chunkPos(), (pos) -> new ConcurrentLinkedQueue<>());

            queue.add(new TileEntity(position, entityData));
        } else {
            chunk.addTileEntity(position, entityData);
            chunk.setSaved(false);
        }
    }

    /**
     * Delete all tile entities for a chunk, only done when the chunk is also unloaded. Note that this only related to
     * tile entities sent in the update-tile-entity packets, ones sent with the chunk will only be stored in the chunk.
     * @param location the position of the chunk for which we can delete tile entities.
     */
    public void deleteTileEntities(Coordinate2D location) {
        tileEntities.remove(location);
    }

    /**
     * Need a non-static method to do this as we cannot otherwise call notify
     */
    public synchronized void addChunk(DataTypeProvider provider) {
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

        factory = getInstance();
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
    private void readChunkDataPacket(DataTypeProvider dataProvider) {
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

        chunk.parse(dataProvider, full);

        // Add any tile entities that were sent before the chunk was parsed. We cannot delete the tile entities yet
        // (so we cannot remove them from the queue) as they are not always re-sent when the chunk is re-sent. (?)
        if (tileEntities.containsKey(chunkPos)) {
            Queue<TileEntity> queue = tileEntities.get(chunkPos);
            for (TileEntity ent : queue) {
                chunk.addTileEntity(ent.getKey(), ent.getValue());
            }
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

    private class TileEntity extends Pair<Coordinate3D, SpecificTag> {
        public TileEntity(Coordinate3D key, SpecificTag value) {
            super(key, value);
        }
    }
}
