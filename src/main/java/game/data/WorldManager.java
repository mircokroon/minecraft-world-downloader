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

/**
 * Manage the world, including saving, parsing and updating the GUI.
 */
public class WorldManager extends Thread {
    private final static int SAVE_DELAY = 20 * 1000;
    private static Map<Coordinate2D, Region> regions = new HashMap<>();

    private static WorldManager writer = null;

    private WorldManager() {}

    /**
     * Read from the save path to see which chunks have been saved already.
     */
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

    /**
     * Start the periodic saving service.
     */
    public static void startSaveService() {
        if (writer != null) {
            return;
        }

        writer = new WorldManager();
        writer.start();
    }

    /**
     * Add a parsed chunk to the correct region.
     * @param coordinate the chunk coordinates
     * @param chunk      the chunk
     */
    public synchronized static void addChunk(Coordinate2D coordinate, Chunk chunk) {
        Coordinate2D regionCoordinates = coordinate.chunkToRegion();

        if (!regions.containsKey(regionCoordinates)) {
            regions.put(regionCoordinates, new Region(regionCoordinates));
        }

        regions.get(regionCoordinates).addChunk(coordinate, chunk);
    }

    /**
     * Get a chunk from the region its in.
     * @param coordinate the global chunk coordinates
     * @return the chunk
     */
    public static Chunk getChunk(Coordinate2D coordinate) {
        return regions.get(coordinate.chunkToRegion()).getChunk(coordinate);
    }

    /**
     * Loop to call save periodically.
     */
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

    /**
     * Save the world. Will tell all regions to save their chunks.
     */
    private synchronized static void save() {
        GuiManager.setSaving(true);
        final int[] saved = {0};
        regions.values().stream().map(Region::toFile).filter(Objects::nonNull).forEach(el -> {
            try {
                saved[0]++;
                el.write();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        GuiManager.setSaving(false);
    }
}
