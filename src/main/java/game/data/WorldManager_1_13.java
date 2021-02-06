package game.data;

import game.data.chunk.Chunk;

/**
 * For versions 1.12 and 1.13, the server does not inform the client of the render distance. We will instead measure
 * the render distance manually by keeping track of chunks the server sends us, and seeing what the largest distance
 * to any of those chunks is. When the world is saved, measuring is completed if a nonzero value was found. After this
 * the class is the same as it's parent.
 */
public class WorldManager_1_13 extends WorldManager {
    public WorldManager_1_13() { }

    boolean measuringRenderDistance = true;
    int maxDistance = 0;

    @Override
    public void loadChunk(Chunk chunk, boolean drawInGui) {
        super.loadChunk(chunk, drawInGui);
        if (!measuringRenderDistance) {
            return;
        }

        if (getPlayerPosition().getX() == 0 && getPlayerPosition().getZ() == 0) {
            return;
        }

        int dist = chunk.location.blockDistance(getPlayerPosition().globalToChunk());
        if (dist > maxDistance && maxDistance < 32) {
            maxDistance = dist;
        }
    }

    @Override
    public void save() {
        super.save();
        if (!measuringRenderDistance || maxDistance == 0) {
            return;
        }
        System.out.println("Server render distance seems to be " + maxDistance + ". Sending chunks from beyond that range.");
        measuringRenderDistance = false;
        getRenderDistanceExtender().setServerDistance(maxDistance);
    }
}
