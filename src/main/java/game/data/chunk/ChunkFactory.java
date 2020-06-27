package game.data.chunk;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.entity.Entity;
import game.data.chunk.version.Chunk_1_12;
import game.data.chunk.version.Chunk_1_13;
import game.data.chunk.version.Chunk_1_14;
import game.data.chunk.version.Chunk_1_15;
import game.data.chunk.version.Chunk_1_16;
import packets.DataTypeProvider;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Class responsible for creating chunks.
 */
public class ChunkFactory extends Thread {
    private static ChunkFactory factory;

    private ConcurrentLinkedQueue<DataTypeProvider> unparsedChunks;
    private ConcurrentMap<Coordinate2D, ConcurrentLinkedQueue<TileEntity>> tileEntities;
    private ConcurrentMap<Coordinate2D, ConcurrentLinkedQueue<Integer>> chunkEntities;
    private ConcurrentMap<Integer, Entity> entities;

    private List<EntityParserPair> unparsedEntities;

    private boolean threadStarted = false;

    public static ChunkFactory getInstance() {
        if (factory == null) {
            factory = new ChunkFactory();
        }
        return factory;
    }

    private ChunkFactory() {
        this.tileEntities = new ConcurrentHashMap<>();
        this.chunkEntities = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
        this.unparsedChunks = new ConcurrentLinkedQueue<>();
        this.unparsedEntities = new LinkedList<>();
    }

    /**
     * Add an unparsed entity.
     */
    public void addEntity(DataTypeProvider provider, Function<DataTypeProvider, Entity> parser) {
        if (WorldManager.getEntityMap() != null) {
            addEntity(parser.apply(provider));
        } else {
            this.unparsedEntities.add(new EntityParserPair(provider, parser));
        }
    }

    /**
     * Parse all entities that were added to the entity list before
     */
    public void parseEntities() {
        this.unparsedEntities.forEach(el -> addEntity(el.parse()));
    }

    /**
     * Update a regular entity that was given individually. If the entity is null, do nothing as its an unknown type.
     * @param ent the entity object
     */
    public void addEntity(Entity ent) {
        if (ent == null) { return; }

        Coordinate2D chunkPos = ent.getPosition().globalToChunk();
        Chunk chunk = WorldManager.getChunk(chunkPos);

        entities.put(ent.getId(), ent);

        // if the chunk doesn't exist yet, add it to the queue to process later
        if (chunk == null) {
            Queue<Integer> queue = chunkEntities.computeIfAbsent(chunkPos, (pos) -> new ConcurrentLinkedQueue<>());

            queue.add(ent.getId());
        } else {
            chunk.addEntity(ent);
            chunk.setSaved(false);
        }
    }

    public Entity getEntity(int key) {
        return entities.getOrDefault(key, null);
    }

    /**
     * Update a tile entity that was given individually.
     * @param position the uncorrected position of the tile entity
     * @param entityData the NBT data of the entity
     */
    public void updateTileEntity(Coordinate3D position, SpecificTag entityData) {
        position.offsetGlobal();

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
        ChunkFactory factory = getInstance();
        if (factory.threadStarted) {
            return;
        }

        factory.start();
    }

    /**
     * Periodically check if there are unparsed chunks, and if so, parse them.
     */
    @Override
    public synchronized void run() {
        threadStarted = true;

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

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = getVersionedChunk(chunkPos);

            WorldManager.loadChunk(chunkPos, chunk, true);
        } else {
            chunk = WorldManager.getChunk(new Coordinate2D(chunkPos.getX(), chunkPos.getZ()));

            // if we don't have the partial chunk (anymore?), just make one from scratch
            if (chunk == null) {
                chunk = getVersionedChunk(chunkPos);
            }

            chunk.markAsNew();
            chunk.setSaved(false);
        }

        chunk.parse(dataProvider, full);

        // Add any tile entities that were sent before the chunk was parsed. We cannot delete the tile entities yet
        // (so we cannot remove them from the queue) as they are not always re-sent when the chunk is re-sent. (?)
        if (tileEntities.containsKey(chunkPos)) {
            Queue<TileEntity> queue = tileEntities.get(chunkPos);
            for (TileEntity ent : queue) {
                chunk.addTileEntity(ent.getPosition(), ent.getTag());
            }
        }

        if (chunkEntities.containsKey(chunkPos)) {
            Queue<Integer> queue = chunkEntities.get(chunkPos);
            for (Integer entId : queue) {
                chunk.addEntity(entities.get(entId));
            }
        }
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(Coordinate2D chunkPos) {
        if (Game.getProtocolVersion() >= 735) {
            return new Chunk_1_16(chunkPos.getX(), chunkPos.getZ());
        } else  if (Game.getProtocolVersion() >= 550) {
            return new Chunk_1_15(chunkPos.getX(), chunkPos.getZ());
        } else if (Game.getProtocolVersion() >= 440) {
            return new Chunk_1_14(chunkPos.getX(), chunkPos.getZ());
        } else if (Game.getProtocolVersion() >= 341) {
            return new Chunk_1_13(chunkPos.getX(), chunkPos.getZ());
        } else {
            return new Chunk_1_12(chunkPos.getX(), chunkPos.getZ());
        }
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(int dataVersion, Coordinate2D chunkPos) {
        if (dataVersion >= 2566) {
            return new Chunk_1_16(chunkPos.getX(), chunkPos.getZ());
        } else if (dataVersion >= 2200) {
            return new Chunk_1_15(chunkPos.getX(), chunkPos.getZ());
        } else if (dataVersion >= 1901) {
            return new Chunk_1_14(chunkPos.getX(), chunkPos.getZ());
        } else if (dataVersion >= 1444) {
            return new Chunk_1_13(chunkPos.getX(), chunkPos.getZ());
        } else {
            return new Chunk_1_12(chunkPos.getX(), chunkPos.getZ());
        }
    }

    /**
     * Delete all tile entities for a chunk, only done when the chunk is also unloaded. Note that this only related to
     * tile entities sent in the update-tile-entity packets, ones sent with the chunk will only be stored in the chunk.
     * @param location the position of the chunk for which we can delete tile entities.
     */
    public void deleteAllEntities(Coordinate2D location) {
        tileEntities.remove(location);

        // delete regular entities as well, some might not unload with the origin chunk but we'll ignore that for now
        Queue<Integer> queue = chunkEntities.get(location);
        if (queue != null) {
            for (Integer entId : queue) {
                entities.remove(entId);
            }
            chunkEntities.remove(location);
        }
    }

    public Chunk fromNbt(NamedTag tag, Coordinate2D location) {
        int dataVersion = tag.getTag().get("DataVersion").intValue();
        Chunk chunk = getVersionedChunk(dataVersion, location);

        chunk.parse(tag.getTag());
        chunk.setSaved(true);

        return chunk;
    }

    private class TileEntity {
        Coordinate3D position;
        SpecificTag tag;

        public TileEntity(Coordinate3D position, SpecificTag tag) {
            this.position = position;
            this.tag = tag;
        }

        public Coordinate3D getPosition() {
            return position;
        }

        public SpecificTag getTag() {
            return tag;
        }
    }
}

class EntityParserPair {
    DataTypeProvider provider;
    Function<DataTypeProvider, Entity> parser;

    public EntityParserPair(DataTypeProvider provider, Function<DataTypeProvider, Entity> parser) {
        this.provider = provider;
        this.parser = parser;
    }

    public Entity parse() {
        return parser.apply(provider);
    }
}
