package game.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class RenderDistanceExtender extends Thread {
    private static final Coordinate2D POS_INIT = new Coordinate2D(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int extendedDistance;
    public int serverDistance = 32;
    private int perRow = 0;

    private Coordinate2D playerChunk = POS_INIT;
    private Coordinate2D newPlayerChunk = new Coordinate2D(0, 0);

    private final Set<Coordinate2D> activeChunks;
    private final WorldManager worldManager;

    public RenderDistanceExtender(WorldManager worldManager, int extendedDistance) {
        this.worldManager = worldManager;
        this.extendedDistance = extendedDistance;

        this.activeChunks = new HashSet<>();
        this.start();
    }

    /**
     * When the server connects it will set the render distance.
     */
    public void setServerDistance(int serverDistance) {
        this.serverDistance = serverDistance;
        this.perRow = extendedDistance * 2 + 1;
        this.activeChunks.clear();
        this.playerChunk = POS_INIT;
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

            if (Math.abs(difference.getX()) + Math.abs(difference.getZ()) == 1) {
                updateSingleRow(difference);
            } else {
                updateFull();
            }
        }
    }

    public void setServerRenderDistance(int distance) {
        this.serverDistance = distance;
    }

    public void updatePlayerPos(Coordinate2D newPos) {
        Coordinate2D newChunkPos = newPos.globalToChunk();
        if (!playerChunk.equals(newChunkPos)) {
            this.newPlayerChunk = newChunkPos;

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
     * In case of teleports or spawns we will have to consider all the chunks instead.
     */
    private void updateFull() {
        Collection<Coordinate2D> desired = new HashSet<>();
        Collection<Coordinate2D> toUnload = new HashSet<>(activeChunks);

        for (int x = -extendedDistance; x <= extendedDistance; x++) {
            for (int z = -extendedDistance; z <= extendedDistance; z++) {
                if (inServerDistance(x, z)) { continue; }

                Coordinate2D chunkCoords = playerChunk.add(x, z);
                if (!activeChunks.contains(chunkCoords))

                desired.add(chunkCoords);
                toUnload.remove(chunkCoords);
            }
        }

        worldManager.unloadChunks(toUnload);
        activeChunks.addAll(worldManager.loadChunks(desired));

        activeChunks.removeAll(toUnload);
    }

    private boolean inServerDistance(int x, int z) {
        return x >= -serverDistance && z >= -serverDistance && x <= serverDistance && z <= serverDistance;
    }
}
