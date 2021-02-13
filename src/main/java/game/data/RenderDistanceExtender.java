package game.data;

import config.Config;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class RenderDistanceExtender extends Thread {
    private static final Coordinate2D POS_INIT = new Coordinate2D(Integer.MAX_VALUE, Integer.MAX_VALUE);

    // for distance measuring
    private boolean measuringRenderDistance;
    private int maxDistance;

    private int extendedDistance;
    public int serverDistance;
    private int perRow;

    private Coordinate2D playerChunk = POS_INIT;
    private Coordinate2D newPlayerChunk = new Coordinate2D(0, 0);

    private final Set<Coordinate2D> activeChunks;
    private final WorldManager worldManager;
    private boolean invalidated = false;
    private boolean active = false;

    public RenderDistanceExtender(WorldManager worldManager, int extendedDistance) {
        this.reset();

        this.worldManager = worldManager;
        this.extendedDistance = extendedDistance;

        this.activeChunks = new HashSet<>();
        this.start();
    }

    private void reset() {
        this.serverDistance = 32;
        this.perRow = 0;
        this.measuringRenderDistance = true;
        this.maxDistance = 0;
    }

    public void setServerReportedRenderDistance(int serverDistance) {
        if (Config.doMeasureRenderDistance()) {
            System.out.println("Ignored server reported render distance of " + serverDistance);
            return;
        }

        if (serverDistance >= 28) {
            System.out.println("Server seems to be running at abnormally high render distance of " + serverDistance
                    + ". Run with --measure-render-distance to ignore this value.");
        }

        this.setServerDistance(serverDistance);
    }

    /**
     * When the server connects it will set the render distance.
     */
    private void setServerDistance(int serverDistance) {
        this.serverDistance = serverDistance;
        this.perRow = extendedDistance * 2 + 1;
        this.activeChunks.clear();
        this.playerChunk = POS_INIT;
        this.measuringRenderDistance = false;

        // only set active to true if the extended distance is actually greater than the server distance
        this.active = this.serverDistance < this.extendedDistance;
    }

    @Override
    public void run() {
        super.run();
        this.setPriority(Thread.MIN_PRIORITY);

        while (true) synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Coordinate2D difference = this.newPlayerChunk.subtract(this.playerChunk);
            this.playerChunk = this.newPlayerChunk;

            // if we moved just 1 chunk from the previous one
            if (!invalidated && Math.abs(difference.getX()) + Math.abs(difference.getZ()) == 1) {
                updateSingleRow(difference);
            } else {
                updateFull();
            }
        }
    }

    public void updatePlayerPos(Coordinate2D newPos) {
        Coordinate2D newChunkPos = newPos.globalToChunk();
        if (measuringRenderDistance) {
            this.playerChunk = newChunkPos;
        } else if (!playerChunk.equals(newChunkPos)) {
            this.newPlayerChunk = newChunkPos;

            // don't start sending chunks before we know the server distance (and its actually smaller than the
            // extended distance)
            if (!active) {
                return;
            }

            synchronized (this) {
                this.notify();
            }
        }
    }

    /**
     * In most cases, the player position will only change by a single chunk at a time. We can handle these much more
     * efficiently by only consider the row at the start and end of the area.
     * @param direction the direction in which the player moved.
     */
    private void updateSingleRow(Coordinate2D direction) {
        if (direction.getX() != 0) {
            int x = direction.getX() * extendedDistance;
            updateRow(z -> playerChunk.add(x, z), z -> playerChunk.add(-x, z));
        } else {
            int z = direction.getZ() * extendedDistance;
            updateRow(x -> playerChunk.add(x, z), x -> playerChunk.add(-x, z));
        }
    }

    private void updateRow(Function<Integer, Coordinate2D> makeCoord, Function<Integer, Coordinate2D> makeRemoveCoord) {
        Collection<Coordinate2D> desired = new ArrayList<>(perRow);
        Collection<Coordinate2D> toUnload = new ArrayList<>(perRow);
        for (int other = -extendedDistance; other <= extendedDistance; other++) {
            Coordinate2D coordAdd = makeCoord.apply(other);
            if (!activeChunks.contains(coordAdd)) {
                desired.add(coordAdd);
            }

            Coordinate2D coordRemove = makeRemoveCoord.apply(other);
            if (activeChunks.contains(coordRemove)) {
                activeChunks.remove(coordRemove);
                toUnload.add(coordRemove);
            }
        }
        worldManager.unloadChunks(toUnload);
        activeChunks.addAll(worldManager.loadChunks(desired));
    }

    /**
     * If called, the server changed dimension and will need all chunks to be sent.
     */
    public void invalidateChunks() {
        invalidated = true;
        this.activeChunks.clear();

        if (measuringRenderDistance) {
            return;
        }

        synchronized (this) {
            notify();
        }
    }


    /**
     * In case of teleports or spawns we will have to consider all the chunks.
     */
    private void updateFull() {
        invalidated = false;
        Collection<Coordinate2D> desired = new HashSet<>();
        Collection<Coordinate2D> toUnload = new HashSet<>(activeChunks);

        for (int x = -extendedDistance; x <= extendedDistance; x++) {
            for (int z = -extendedDistance; z <= extendedDistance; z++) {
                if (inServerDistance(x, z)) { continue; }

                Coordinate2D chunkCoords = playerChunk.add(x, z);
                if (!activeChunks.contains(chunkCoords)) {
                    desired.add(chunkCoords);
                }
                toUnload.remove(chunkCoords);
            }
        }

        worldManager.unloadChunks(toUnload);
        activeChunks.addAll(worldManager.loadChunks(desired));

        activeChunks.removeAll(toUnload);
    }

    /**
     * Checks if a given chunk coordinate is within the server's render distance.
     */
    private boolean inServerDistance(int x, int z) {
        return x >= -serverDistance && z >= -serverDistance && x <= serverDistance && z <= serverDistance;
    }

    /**
     * Checks whether we have found a suitable distance measure for the server render distance.
     */
    public void checkDistance() {
        if (!measuringRenderDistance || maxDistance == 0) {
            return;
        }
        System.out.println("Server render distance seems to be " + maxDistance + ". Sending chunks from beyond that range.");
        setServerDistance(maxDistance);
    }

    /**
     * Notifies of newly loaded chunks so we can measure the server render distance.
     */
    public void updateDistance(CoordinateDim2D location) {
        if (!measuringRenderDistance) {
            return;
        }

        if (playerChunk.getX() == 0 && playerChunk.getZ() == 0) {
            return;
        }

        int dist = location.blockDistance(playerChunk);
        if (dist > maxDistance && maxDistance < 32 && dist < 32) {
            maxDistance = dist;
        }
    }

    public void resetConnection() {
        this.reset();
        this.invalidateChunks();
    }

    public void setExtendedDistance(int val) {
        this.extendedDistance = val;

        invalidateChunks();
    }
}
