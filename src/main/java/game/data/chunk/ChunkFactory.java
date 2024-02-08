package game.data.chunk;

import config.Config;
import config.Option;
import config.Version;
import config.VersionReporter;
import game.data.chunk.version.*;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.WorldManager;
import packets.DataTypeProvider;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.util.*;
import java.util.concurrent.*;

/**
 * Class responsible for creating chunks.
 */
public class ChunkFactory {
    private Map<CoordinateDim2D, UnparsedChunk> unparsedChunks;

    private ThreadPoolExecutor executor;

    public ChunkFactory() {
        clear();
    }

    public void clear() {
        this.unparsedChunks = new ConcurrentHashMap<>();

        // same as newSingleThreadExecutor except we can observe the queue size
        this.executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            (r) -> new Thread(r, "Chunk Parser Service")
        );
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(int dataVersion, CoordinateDim2D chunkPos) {
        return VersionReporter.select(dataVersion, Chunk.class,
              Option.of(Version.V1_20, () -> new Chunk_1_20(chunkPos, dataVersion)),
              Option.of(Version.V1_18, () -> new Chunk_1_18(chunkPos, dataVersion)),
              Option.of(Version.V1_17, () -> new Chunk_1_17(chunkPos, dataVersion)),
              Option.of(Version.V1_16, () -> new Chunk_1_16(chunkPos, dataVersion)),
              Option.of(Version.V1_15, () -> new Chunk_1_15(chunkPos, dataVersion)),
              Option.of(Version.V1_14, () -> new Chunk_1_14(chunkPos, dataVersion)),
              Option.of(Version.V1_13, () -> new Chunk_1_13(chunkPos, dataVersion)),
              Option.of(Version.V1_12, () -> new Chunk_1_12(chunkPos, dataVersion))
        );
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
            getUnparsedIfFresh(chunkPos).addTileEntity(new TileEntity(position, entityData));
        } else {
            chunk.addBlockEntity(position, entityData);
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

        Runnable r = () -> {
            CoordinateDim2D chunkPos = new CoordinateDim2D(provider.readInt(), provider.readInt(), WorldManager.getInstance().getDimension());
            getUnparsed(chunkPos).setProvider(provider);

            this.parse();
        };

        if (executor != null) {
            executor.execute(r);
        } else {
            r.run();
        }
    }

    private void parse() {
        for (CoordinateDim2D k : unparsedChunks.keySet()) {
            try {
                boolean doRemove = readChunkDataPacket(unparsedChunks.get(k));

                if (doRemove) {
                    unparsedChunks.remove(k);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("Chunk could not be parsed!");
                unparsedChunks.remove(k);
            }
        }
    }

    public static Chunk parseChunk(UnparsedChunk parser, WorldManager worldManager) {
        DataTypeProvider dataProvider = parser.provider;
        CoordinateDim2D chunkPos = parser.location;

        Chunk chunk = worldManager.getChunk(chunkPos);
        if (chunk == null) {
            chunk = getVersionedChunk(chunkPos);
            worldManager.loadChunk(chunk, true, true);
        }

        chunk.parse(dataProvider);

        return chunk;
    }
    /**
     * Parse a chunk data packet. Largely based on: https://wiki.vg/Protocol
     */
    private boolean readChunkDataPacket(UnparsedChunk parser) {
        if (parser.parsingInProgress) { return false; }

        // If no chunk parser is present, the unparsed chunk will be removed if no chunk parser is added within a time
        // limit. This can only happen in rare cases where data from a chunk arrives before the chunk.
        if (parser.provider == null) {
            return parser.isStale();
        }
        parser.parsingInProgress = true;

        Chunk chunk = parseChunk(parser, WorldManager.getInstance());

        // Add any tile entities that were sent before the chunk was parsed. We cannot delete the tile entities yet
        // (so we cannot remove them from the queue) as they are not always re-sent when the chunk is re-sent. (?)
        if (parser.tileEntities != null) {
            for (TileEntity ent : parser.tileEntities) {
                chunk.addBlockEntity(ent.getPosition(), ent.getTag());
            }
        }

        if (parser.lighting != null) {
            chunk.updateLight(parser.lighting);
        }
        return true;
    }

    /**
     * Returns a chunk of the correct version.
     * @param chunkPos the position of the chunk
     * @return the chunk matching the given version
     */
    private static Chunk getVersionedChunk(CoordinateDim2D chunkPos) {
        return getVersionedChunk(Config.versionReporter().getDataVersion(), chunkPos);
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
    }

    public void updateLight(CoordinateDim2D coords, DataTypeProvider provider) {
        UnparsedChunk unparsed = getUnparsedIfFresh(coords);
        unparsed.lighting = provider;
    }

    public void runOnFactoryThread(Runnable r) {
        executor.execute(r);
    }

    public void unloadChunk(CoordinateDim2D coord) {
        UnparsedChunk unparsedChunk = this.unparsedChunks.get(coord);
        if (unparsedChunk != null) {
            unparsedChunk.shouldUnload = true;
        }
    }

    protected UnparsedChunk getUnparsed(CoordinateDim2D location) {
        return unparsedChunks.computeIfAbsent(location, UnparsedChunk::new);
    }

    protected UnparsedChunk getUnparsedIfFresh(CoordinateDim2D location) {
        UnparsedChunk current = unparsedChunks.get(location);

        // if the old one is stale, remove it
        if (current == null || current.shouldUnload) {
            current = new UnparsedChunk(location);
            unparsedChunks.put(location, current);
        }

        return current;
    }

    public int countQueuedChunks() {
        return executor.getQueue().size();
    }
}

/**
 * Hold unparsed chunks and any separately sent data that needs to be added to it (tile entities and light data).
 */
class UnparsedChunk {
    private static final long MAX_WAIT_TIME = 1000 * 10;

    private final long initTime;

    CoordinateDim2D location;
    DataTypeProvider provider;
    DataTypeProvider lighting;
    Queue<TileEntity> tileEntities;
    boolean shouldUnload;
    boolean parsingInProgress;

    public UnparsedChunk(CoordinateDim2D location) {
        this.location = location;
        this.initTime = System.currentTimeMillis();
    }

    public void addTileEntity(TileEntity tileEntity) {
        if (this.tileEntities == null) {
            this.tileEntities = new ConcurrentLinkedQueue<>();
        }
        tileEntities.add(tileEntity);
    }

    public void setProvider(DataTypeProvider provider) {
        this.provider = provider;
    }

    /**
     * If the chunk data itself does not arrive within a few seconds, the lighting/tile entity datA is discarded.
     */
    public boolean isStale() {
        return System.currentTimeMillis() - initTime > MAX_WAIT_TIME;
    }
}

class TileEntity {
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