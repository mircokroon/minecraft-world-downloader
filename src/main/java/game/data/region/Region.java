package game.data.region;

import game.data.WorldManager;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import game.data.chunk.ChunkFactory;
import game.data.dimension.Dimension;
import gui.GuiManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class relating to a region (32x32 chunk area), corresponds to one MCA file.
 */
public class Region {
    public static final Region EMPTY = new Region(new CoordinateDim2D(0, 0, Dimension.OVERWORLD));
    private final int UNLOAD_RANGE = 24;
    private Map<Coordinate2D, Chunk> chunks;
    private CoordinateDim2D regionCoordinates;

    private boolean updatedSinceLastWrite;
    private Set<Coordinate2D> toDelete;

    /**
     * Initialise the region with the given coordinates.
     * @param regionCoordinates coordinates of the region (so global coordinates / 16 / 32)
     */
    public Region(CoordinateDim2D regionCoordinates) {
        this.regionCoordinates = regionCoordinates;
        this.chunks = new ConcurrentHashMap<>();
        this.updatedSinceLastWrite = false;
        this.toDelete = new HashSet<>();
    }

    /**
     * Add a chunk to the region.
     * @param coordinate the coordinate of the new chunk
     * @param chunk      the chunk to add
     */
    public void addChunk(Coordinate2D coordinate, Chunk chunk, boolean overrideExisting) {
        if (overrideExisting) {
            chunks.put(coordinate, chunk);
        } else {
            chunks.putIfAbsent(coordinate, chunk);
        }
        updatedSinceLastWrite = true;
    }


    /**
     * Delete chunk when the server tells us to unload it.
     * @param coordinate chunk coordinate to unload
     */
    public void removeChunk(Coordinate2D coordinate) {
        Chunk chunk = chunks.get(coordinate);

        if (chunk == null) {
            return;
        }

        if (chunk.isSaved()) {
            chunks.remove(coordinate);
        } else {
            toDelete.add(coordinate);
        }
        chunk.unload();
    }


    /**
     * Returns true if the region has no chunks in it (e.g. they have all been deleted)
     */
    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    public Chunk getChunk(Coordinate2D coordinate) {
        return chunks.get(coordinate);
    }

    /**
     * Convert this region to an McaFile object. Will delete any chunks out of the render distance if they have already
     * been saved. Will update the Gui with the chunk that's about to be saved.
     * @return the McaFile corresponding to this region
     */
    public McaFile toFile(Coordinate2D playerPos) {
        if (!updatedSinceLastWrite) {
            return null;
        }

        updatedSinceLastWrite = false;

        Map<Integer, ChunkBinary> chunkBinaryMap = new HashMap<>();
        chunks.keySet().forEach(coordinate -> {
            try {
                Chunk chunk = chunks.get(coordinate);

                // mark the chunk for deletion -- this is really only a back-up, the unload-chunk packet sent by the
                // server is what should be used to unload chunks correctly.
                if (!playerPos.isInRange(coordinate, UNLOAD_RANGE)) {
                    toDelete.add(coordinate);
                }

                if (chunk.isSaved()) {
                    return;
                }

                chunk.setSaved(true);
                GuiManager.markChunkSaved(coordinate.addDimension(regionCoordinates.getDimension()));

                // get the chunk in binary format and get its coordinates as an Mca compatible integer. Then add
                // these to the map of chunk binaries.
                ChunkBinary binary = ChunkBinary.fromChunk(chunk);

                if (binary == null) {
                    return;
                }

                Coordinate2D localCoordinate = coordinate.toRegionLocal();
                int pos = 4 * ((localCoordinate.getX() & 31) + (localCoordinate.getZ() & 31) * 32);
                chunkBinaryMap.put(pos, binary);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // delete chunks and their sent-later tile entities
        for (Coordinate2D c : toDelete) {
            WorldManager.getInstance().unloadEntities(c.addDimension(regionCoordinates.getDimension()));
            chunks.remove(c);
        }
        toDelete.clear();

        if (chunkBinaryMap.isEmpty()) {
            return null;
        }
        return new McaFile(regionCoordinates, chunkBinaryMap);
    }

    /**
     * Mark region as having modified chunks.
     */
    public void touch() {
        this.updatedSinceLastWrite = true;
    }

    public int countChunks() {
        return this.chunks.size();
    }
}
