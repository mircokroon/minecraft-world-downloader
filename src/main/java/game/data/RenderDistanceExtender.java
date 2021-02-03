package game.data;

import game.Config;
import game.data.chunk.Chunk;
import game.data.dimension.Dimension;
import game.data.region.McaFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RenderDistanceExtender {
    Coordinate2D playerChunk = new Coordinate2D(0, 0);
    int extendedDistance = 0;
    int serverDistance = 32;

    Set<Coordinate2D> activeChunks;
    WorldManager worldManager;

    public RenderDistanceExtender(WorldManager worldManager, int extendedDistance) {
        this.worldManager = worldManager;
        this.extendedDistance = extendedDistance;

        this.activeChunks = new HashSet<>();
    }

    public void setServerDistance(int serverDistance) {
        this.serverDistance = serverDistance;
        if (this.serverDistance <= this.extendedDistance) {
            this.activeChunks.clear();
        }


    }


    public void setServerRenderDistance(int distance) {
        this.serverDistance = distance;
    }

    public void updatePlayerPos(Coordinate2D newPos) {
        Coordinate2D newChunkPos = newPos.globalToChunk();
        Coordinate2D oldChunkPos = playerChunk;
        if (!playerChunk.equals(newChunkPos)) {
            System.out.println("New chunk");
            this.playerChunk = newChunkPos;

            Coordinate2D difference = oldChunkPos.subtract(newChunkPos);
            if (Math.abs(difference.getX()) + Math.abs(difference.getZ()) == 1) {
                updateSingleRow(difference);
            } else {
                updateFull();
            }
        }
    }

    /**
     * In most cases, the player position will only change by a single chunk at a time. We can handle these much more
     * efficiently so we don't need to consider all the chunks in this scenario.
     * @param direction the direction in which the player moved.
     */
    private void updateSingleRow(Coordinate2D direction) {
        updateFull();
        // TODO
    }

    /**
     * In case of teleports or spawns we will have to consider all the chunks instead.
     */
    private void updateFull() {
        Set<Coordinate2D> desired = new HashSet<>();
        Set<Coordinate2D> toUnload = new HashSet<>(activeChunks);

        for (int x = -extendedDistance; x <= extendedDistance; x++) {
            for (int z = -extendedDistance; z <= extendedDistance; z++) {
                if ((x == -extendedDistance && z == -extendedDistance) || (x == extendedDistance && z == extendedDistance)) {
                    System.out.println("Min: " + x +", " + z + ": " + inServerDistance(x, z));
                }
                if (inServerDistance(x, z)) { continue; }

                Coordinate2D chunkCoords = playerChunk.add(x, z);
                if (!activeChunks.contains(chunkCoords))

                desired.add(chunkCoords);
                toUnload.remove(chunkCoords);
            }
        }

        worldManager.unloadChunks(toUnload);
        Set<Coordinate2D> loaded = worldManager.loadChunks(desired);

        activeChunks.removeAll(toUnload);
        activeChunks.addAll(loaded);
    }

    private boolean inServerDistance(int x, int z) {
        return x >= -serverDistance && z >= -serverDistance && x <= serverDistance && z <= serverDistance;
    }
}
