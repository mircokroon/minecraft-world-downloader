package game.data.chunk;

import config.Config;
import config.Option;
import config.Version;
import config.VersionReporter;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.WorldManager;
import game.data.entity.Entity;
import game.data.chunk.version.Chunk_1_12;
import game.data.chunk.version.Chunk_1_13;
import game.data.chunk.version.Chunk_1_14;
import game.data.chunk.version.Chunk_1_15;
import game.data.chunk.version.Chunk_1_16;
import packets.DataTypeProvider;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Class responsible for creating chunks.
 */
public class ChunkFactory {
    private Queue<ChunkParserPair> unparsedChunks;
    private Map<CoordinateDim2D, ConcurrentLinkedQueue<TileEntity>> tileEntities;

    private ExecutorService executor;

    public ChunkFactory() {
        clear();
    }

    public void clear() {
        this.tileEntities = new ConcurrentHashMap<>();
        this.unparsedChunks = new ConcurrentLinkedQueue<>();

        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Chunk Parser Service"));;
    }

    /**
     * Update a tile entity that was given individually.
     * @param position the uncorrected position of the tile entity
     * @param entityData the NBT data of the entity
     */
    public void updateTileEntity(Coordinate3D position, SpecificTag entityData) {
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
    public void addChunk(DataTypeProvider provider) {
        // if the world manager is currently paused, discard this chunk
        if (WorldManager.getInstance().isPaused()) {
            return;
        }

        unparsedChunks.add(new ChunkParserPair(provider, WorldManager.getInstance().getDimension()));

        // check if executor is defined - there is a rare race condition where the proxy could receive chunks before
        // it is initiated
        if (executor != null) {
            executor.execute(this::parse);
        }
    }

    private void parse() {
        ChunkParserPair parsePair;
        while ((parsePair = getUnparsedChunk()) != null) {
            try {
                readChunkDataPacket(parsePair);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Chunk could not be parsed!");
            }
        }
    }

    /**
     * Gets an unparsed chunk from the list, or null if the list is empty.
     */
    private ChunkParserPair getUnparsedChunk() {
        if (unparsedChunks.isEmpty()) {
            return null;
        }
        return unparsedChunks.remove();
    }

    public static Chunk parseChunk(ChunkParserPair parser, WorldManager worldManager) {
        DataTypeProvider dataProvider = parser.provider;

        CoordinateDim2D chunkPos = new CoordinateDim2D(dataProvider.readInt(), dataProvider.readInt(), parser.dimension);

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = getVersionedChunk(chunkPos);

            worldManager.loadChunk(chunk, true, true);
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
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(CoordinateDim2D chunkPos) {
        return Config.versionReporter().select(Chunk.class,
                Option.of(Version.V1_16, () -> new Chunk_1_16(chunkPos)),
                Option.of(Version.V1_15, () -> new Chunk_1_15(chunkPos)),
                Option.of(Version.V1_14, () -> new Chunk_1_14(chunkPos)),
                Option.of(Version.V1_13, () -> new Chunk_1_13(chunkPos)),
                Option.of(Version.V1_12, () -> new Chunk_1_12(chunkPos))
        );
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(int dataVersion, CoordinateDim2D chunkPos) {
        return VersionReporter.select(dataVersion, Chunk.class,
                Option.of(Version.V1_16, () -> new Chunk_1_16(chunkPos)),
                Option.of(Version.V1_15, () -> new Chunk_1_15(chunkPos)),
                Option.of(Version.V1_14, () -> new Chunk_1_14(chunkPos)),
                Option.of(Version.V1_13, () -> new Chunk_1_13(chunkPos)),
                Option.of(Version.V1_12, () -> new Chunk_1_12(chunkPos))
        );

    }

    public Chunk fromNbt(NamedTag tag, CoordinateDim2D location) {
        int dataVersion = tag.getTag().get("DataVersion").intValue();
        Chunk chunk = getVersionedChunk(dataVersion, location);

        chunk.parse(tag.getTag());
        chunk.setSaved(true);

        return chunk;
    }

    public void reset() {
        this.unparsedChunks.clear();
        this.tileEntities.clear();
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

    public void unloadChunk(CoordinateDim2D coord) {
        this.tileEntities.remove(coord);
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