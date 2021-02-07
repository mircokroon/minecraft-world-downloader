package game.data.chunk;

import game.Config;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.CoordinateDim2D;
import game.data.dimension.Dimension;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Class responsible for creating chunks.
 */
public class ChunkFactory extends Thread {
    private static ChunkFactory factory;

    private ConcurrentLinkedQueue<ChunkParserPair> unparsedChunks;
    private ConcurrentMap<CoordinateDim2D, ConcurrentLinkedQueue<TileEntity>> tileEntities;
    private ConcurrentMap<CoordinateDim2D, ConcurrentLinkedQueue<Integer>> chunkEntities;
    private ConcurrentMap<Integer, Entity> entities;

    private Collection<EntityParser> unparsedEntities;

    private boolean threadStarted = false;

    public static ChunkFactory getInstance() {
        if (factory == null) {
            factory = new ChunkFactory();
        }
        return factory;
    }

    private ChunkFactory() {
        clear();
    }

    public void clear() {
        this.tileEntities = new ConcurrentHashMap<>();
        this.chunkEntities = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
        this.unparsedChunks = new ConcurrentLinkedQueue<>();
        this.unparsedEntities = new ArrayDeque<>();
    }

    /**
     * Add an unparsed entity.
     */
    public void addEntity(DataTypeProvider provider, Function<DataTypeProvider, Entity> parser) {
        if (WorldManager.getInstance().getEntityMap() != null) {
            addEntity(parser.apply(provider), WorldManager.getInstance().getDimension());
        } else {
            this.unparsedEntities.add(new EntityParser(provider, WorldManager.getInstance().getDimension(), parser));
        }
    }

    /**
     * Parse all entities that were added to the entity list before
     */
    public void parseEntities() {
        this.unparsedEntities.forEach(el -> addEntity(el.parse(), el.dimension));
    }

    /**
     * Update a regular entity that was given individually. If the entity is null, do nothing as its an unknown type.
     * @param ent the entity object
     */
    public void addEntity(Entity ent, Dimension dimension) {
        if (ent == null) { return; }

        CoordinateDim2D chunkPos = ent.getPosition().globalToChunk().addDimension(dimension);
        Chunk chunk = WorldManager.getInstance().getChunk(chunkPos);

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
        CoordinateDim2D chunkPos = position.globalToChunk().addDimension(WorldManager.getInstance().getDimension());

        Chunk chunk = WorldManager.getInstance().getChunk(chunkPos);

        // if the chunk doesn't exist yet, add it to the queue to process later
        if (chunk == null) {
            Queue<TileEntity> queue = tileEntities
                .computeIfAbsent(chunkPos, (pos) -> new ConcurrentLinkedQueue<>());

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
        // if the world manager is currently paused, discard this chunk
        if (WorldManager.getInstance().isPaused()) {
            return;
        }

        unparsedChunks.add(new ChunkParserPair(provider, WorldManager.getInstance().getDimension()));
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

        ChunkParserPair provider;
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
    private synchronized ChunkParserPair getUnparsedChunk() {
        if (unparsedChunks.isEmpty()) {
            return null;
        }
        return unparsedChunks.remove();
    }

    public static Chunk parseChunk(ChunkParserPair parser, WorldManager worldManager) {
        DataTypeProvider dataProvider = parser.provider;

        CoordinateDim2D chunkPos = new CoordinateDim2D(dataProvider.readInt(), dataProvider.readInt(), parser.dimension);
        chunkPos.offsetChunk();

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = getVersionedChunk(chunkPos);

            worldManager.loadChunk(chunk, true);
        } else {
            chunk = worldManager.getChunk(new CoordinateDim2D(chunkPos.getX(), chunkPos.getZ(), parser.dimension));

            // if we don't have the partial chunk (anymore?), just make one from scratch
            if (chunk == null) {
                chunk = getVersionedChunk(chunkPos);
            }

            chunk.markAsNew();
            chunk.setSaved(false);
        }

        chunk.parse(dataProvider, full);

        return chunk;
    }
    /**
     * Parse a chunk data packet. Largely based on: https://wiki.vg/Protocol
     */
    private void readChunkDataPacket(ChunkParserPair parser) {
        Chunk chunk = parseChunk(parser, WorldManager.getInstance());

        // Add any tile entities that were sent before the chunk was parsed. We cannot delete the tile entities yet
        // (so we cannot remove them from the queue) as they are not always re-sent when the chunk is re-sent. (?)
        if (tileEntities.containsKey(chunk.location)) {
            Queue<TileEntity> queue = tileEntities.get(chunk.location);
            for (TileEntity ent : queue) {
                chunk.addTileEntity(ent.getPosition(), ent.getTag());
            }
        }

        if (chunkEntities.containsKey(chunk.location)) {
            Queue<Integer> queue = chunkEntities.get(chunk.location);
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
    private static Chunk getVersionedChunk(CoordinateDim2D chunkPos) {
        if (Config.getProtocolVersion() >= 751) {
            return new Chunk_1_16(chunkPos);
        } else  if (Config.getProtocolVersion() >= 550) {
            return new Chunk_1_15(chunkPos);
        } else if (Config.getProtocolVersion() >= 440) {
            return new Chunk_1_14(chunkPos);
        } else if (Config.getProtocolVersion() >= 341) {
            return new Chunk_1_13(chunkPos);
        } else {
            return new Chunk_1_12(chunkPos);
        }
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(int dataVersion, CoordinateDim2D chunkPos) {
        if (dataVersion >= Chunk_1_16.DATA_VERSION) {
            return new Chunk_1_16(chunkPos);
        } else if (dataVersion >= Chunk_1_15.DATA_VERSION) {
            return new Chunk_1_15(chunkPos);
        } else if (dataVersion >= Chunk_1_14.DATA_VERSION) {
            return new Chunk_1_14(chunkPos);
        } else if (dataVersion >= Chunk_1_13.DATA_VERSION) {
            return new Chunk_1_13(chunkPos);
        } else {
            return new Chunk_1_12(chunkPos);
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

    public Chunk fromNbt(NamedTag tag, CoordinateDim2D location) {
        int dataVersion = tag.getTag().get("DataVersion").intValue();
        Chunk chunk = getVersionedChunk(dataVersion, location);

        chunk.parse(tag.getTag());
        chunk.setSaved(true);

        return chunk;
    }

    private static class TileEntity {
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

class ChunkParserPair {
    DataTypeProvider provider;
    Dimension dimension;

    public ChunkParserPair(DataTypeProvider provider, Dimension dimension) {
        this.provider = provider;
        this.dimension = dimension;
    }
}

class EntityParser {
    DataTypeProvider provider;
    Dimension dimension;
    Function<DataTypeProvider, Entity> parser;

    public EntityParser(DataTypeProvider provider, Dimension dimension, Function<DataTypeProvider, Entity> parser) {
        this.dimension = dimension;
        this.provider = provider;
        this.parser = parser;
    }

    public Entity parse() {
        return parser.apply(provider);
    }
}
