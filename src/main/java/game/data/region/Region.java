package game.data.region;

import game.Game;
import game.data.Coordinate2D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import gui.GuiManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class relating to a region (32x32 chunk area), corresponds to one MCA file.
 */
public class Region {
    private final int UNLOAD_RANGE = 40;
    private Map<Coordinate2D, Chunk> chunks;
    private Coordinate2D regionCoordinates;

    private boolean updatedSinceLastWrite;

    /**
     * Initialise the region with the given coordinates.
     * @param regionCoordinates coordinates of the region (so global coordinates / 16 / 32)
     */
    public Region(Coordinate2D regionCoordinates) {
        this.regionCoordinates = regionCoordinates;
        this.chunks = new HashMap<>();
        this.updatedSinceLastWrite = false;
    }

    /**
     * Add a chunk to the region.
     * @param coordinate the coordinate of the new chunk
     * @param chunk      the chunk to add
     */
    public void addChunk(Coordinate2D coordinate, Chunk chunk) {
        chunks.put(coordinate, chunk);
        updatedSinceLastWrite = true;
    }

    public Chunk getChunk(Coordinate2D coordinate) {
        return chunks.get(coordinate);
    }

    /**
     * Convert this region to an McaFile object. Will delete any chunks out of the render distance if they have already
     * been saved. Will update the Gui with the chunk that's about to be saved.
     * @return the McaFile corresponding to this region
     */
    public McaFile toFile() {
        if (!updatedSinceLastWrite) {
            return null;
        }

        updatedSinceLastWrite = false;
        Coordinate2D playerPos = Game.getPlayerPosition().chunkPos();

        Map<Integer, ChunkBinary> chunkBinaryMap = new HashMap<>();
        List<Coordinate2D> saved = new ArrayList<>();
        List<Coordinate2D> toDelete = new ArrayList<>();
        chunks.keySet().forEach(coordinate -> {
            try {
                Chunk chunk = chunks.get(coordinate);
                if (!playerPos.isInRange(coordinate, UNLOAD_RANGE)) {
                    toDelete.add(coordinate);
                }

                if (chunk.isSaved()) {
                    return;
                }

                chunk.setSaved(true);
                saved.add(coordinate);

                // get the chunk in binary format and get its coordinates as an Mca compatible integer. Then add
                // these to the map of chunk binaries.
                ChunkBinary binary = ChunkBinary.fromChunk(chunk);

                if (binary == null) {
                    return;
                }

                Coordinate2D localCoordinate = coordinate.toRegionLocal();
                int pos = 4 * ((localCoordinate.getX() & 31) + (localCoordinate.getZ() & 31) * 32);
                chunkBinaryMap.put(pos, binary);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        GuiManager.setChunksSaved(saved);

        for (Coordinate2D c : toDelete) {
            chunks.remove(c);
        }

        if (chunkBinaryMap.isEmpty()) {
            return null;
        }
        return new McaFile(regionCoordinates, chunkBinaryMap);
    }
}
