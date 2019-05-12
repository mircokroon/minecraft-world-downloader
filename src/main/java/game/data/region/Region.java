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

public class Region {
    private final int UNLOAD_RANGE = 40;
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
                ChunkBinary binary = ChunkBinary.fromChunk(chunk);
                saved.add(coordinate);



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
            System.out.println("Removing chunk " + c);
        }

        return new McaFile(regionCoordinates, chunkBinaryMap);
    }
}
