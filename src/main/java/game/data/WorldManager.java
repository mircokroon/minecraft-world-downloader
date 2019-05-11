package game.data;

import game.data.chunk.Chunk;
import game.data.region.Region;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldManager extends Thread {
    private  final static int SAVE_DELAY = 30 * 1000;
    private static Map<Coordinate2D, Region> regions = new HashMap<>();

    private static WorldManager writer = null;

    private WorldManager() {}

    private synchronized static void save() {
        System.out.print("Saving... ");
        long start = System.currentTimeMillis();
        final int[] saved = {0};
        regions.values().stream().map(Region::toFile).filter(Objects::nonNull).forEach(el -> {
            try {
                saved[0]++;
                el.write();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        int timeTaken = (int) ((System.currentTimeMillis() - start) / 1e3);
        System.out.println("\rSaved " + saved[0] + " files in " + timeTaken + "s");
    }

    public static void startSaveService() {
        if (writer != null) {
            return;
        }

        writer = new WorldManager();
        writer.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(SAVE_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            save();
        }
    }

    public synchronized static void addChunk(Coordinate2D coordinate, Chunk chunk) {
        Coordinate2D regionCoordinates = coordinate.toRegion();

        if (!regions.containsKey(regionCoordinates)) {
            regions.put(regionCoordinates, new Region(regionCoordinates));
        }

        regions.get(regionCoordinates).addChunk(coordinate, chunk);
    }

    public static Chunk getChunk(Coordinate2D coordinate) {
        return regions.get(coordinate.toRegion()).getChunk(coordinate);
    }

}
