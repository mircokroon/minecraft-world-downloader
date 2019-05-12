package game.data.region;

import game.data.Coordinate2D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import gui.GuiManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Region {
    private Map<Coordinate2D, Chunk> chunks;
    private Coordinate2D regionCoordinates;

    private boolean updatedSinceLastWrite;

    public Region(Coordinate2D regionCoordinates) {
        this.regionCoordinates = regionCoordinates;
        this.chunks = new HashMap<>();
        this.updatedSinceLastWrite = false;
    }

    public void addChunk(Coordinate2D coordinate, Chunk chunk) {
        chunks.put(coordinate, chunk);
        GuiManager.setChunkLoaded(coordinate);
        updatedSinceLastWrite = true;
    }

    public Chunk getChunk(Coordinate2D coordinate) {
        return chunks.get(coordinate);
    }

    public McaFile toFile() {
        if (!updatedSinceLastWrite) {
            return null;
        }

        updatedSinceLastWrite = false;

        Map<Integer, ChunkBinary> chunkBinaryMap = new HashMap<>();
        chunks.keySet().forEach(coordinate -> {
            try {
                ChunkBinary binary = ChunkBinary.fromChunk(chunks.get(coordinate));
                GuiManager.setChunkSaved(coordinate);

                Coordinate2D localCoordinate = coordinate.toRegionLocal();
                int pos = 4 * ((localCoordinate.getX() & 31) + (localCoordinate.getZ() & 31) * 32);
                chunkBinaryMap.put(pos, binary);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return new McaFile(regionCoordinates, chunkBinaryMap);
    }
}
