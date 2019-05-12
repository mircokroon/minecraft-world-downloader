package game.data;

import game.Game;
import game.data.chunk.Chunk;
import game.data.region.McaFile;
import game.data.region.Region;
import gui.GuiManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorldManager extends Thread {
    private  final static int SAVE_DELAY = 20 * 1000;
    private static Map<Coordinate2D, Region> regions = new HashMap<>();

    private static WorldManager writer = null;

    private WorldManager() {}

    public static void loadExistingChunks() throws IOException {
        Path exportDir = Paths.get(Game.getExportDirectory(), "region");

        List<Coordinate2D> existing = Files.walk(exportDir)
            .filter(el -> el.getFileName().toString().endsWith(".mca"))
            .map(Path::toFile)
            .filter(el -> el.length() > 0)
            .map(el -> {
                try {
                    return new McaFile(el);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .flatMap(el -> el.getChunkPositions().stream()).collect(Collectors.toList());

        GuiManager.setChunksSaved(existing);
    }

    private synchronized static void save() {
        GuiManager.setSaving(true);
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
        GuiManager.setSaving(false);
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
        Coordinate2D regionCoordinates = coordinate.chunkToRegion();

        if (!regions.containsKey(regionCoordinates)) {
            regions.put(regionCoordinates, new Region(regionCoordinates));
        }

        regions.get(regionCoordinates).addChunk(coordinate, chunk);
    }

    public static Chunk getChunk(Coordinate2D coordinate) {
        return regions.get(coordinate.chunkToRegion()).getChunk(coordinate);
    }

}
