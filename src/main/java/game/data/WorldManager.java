package game.data;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;

import game.Game;
import game.data.chunk.Chunk;
import game.data.region.McaFile;
import game.data.region.Region;
import gui.GuiManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manage the world, including saving, parsing and updating the GUI.
 */
public class WorldManager extends Thread {
    private final static int SAVE_DELAY = 20 * 1000;
    private static Map<Coordinate2D, Region> regions = new ConcurrentHashMap<>();

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
     * Save the level.dat file so the world can be easily opened. If one doesn't exist, use the default one from
     * the resource folder.
     */
    private static void saveLevelData() throws IOException {
        File levelDat = Paths.get(Game.getExportDirectory(), "level.dat").toFile();

        // if there is no level.dat yet, make one from the default
        InputStream fileInput;
        if (levelDat.isFile()) {
            fileInput = new FileInputStream(levelDat);
        } else {
            fileInput = WorldManager.class.getClassLoader().getResourceAsStream("level.dat");
        }

        // get default level.dat
        CompoundTag level = (CompoundTag) new NBTInputStream(fileInput, true).readTag();
        CompoundMap data = (CompoundMap) level.getValue().get("Data").getValue();

        // add the player's position
        Coordinate3D playerPosition = Game.getPlayerPosition();
        if (playerPosition != null) {
            CompoundTag player = new CompoundTag("Player", new CompoundMap());
            CompoundMap playerMap = (CompoundMap) data.getOrDefault("Player",  player).getValue();

            playerMap.put(new ListTag<>("Pos", DoubleTag.class, Arrays.asList(
                new DoubleTag("X", playerPosition.getX() * 1.0),
                new DoubleTag("Y", playerPosition.getY() * 1.0),
                new DoubleTag("Z", playerPosition.getZ() * 1.0)
            )));

            // set the world spawn to match the last known player location
            data.put(new IntTag("SpawnX", playerPosition.getX()));
            data.put(new IntTag("SpawnY", playerPosition.getY()));
            data.put(new IntTag("SpawnZ", playerPosition.getZ()));
        }

        // add the seed & last played time
        data.put(new LongTag("RandomSeed", Game.getSeed()));
        data.put(new LongTag("LastPlayed", System.currentTimeMillis()));

        // write the file
        NBTOutputStream output = new NBTOutputStream(new FileOutputStream(levelDat), true);
        output.writeTag(level);
        output.close();
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
    public static void loadChunk(Coordinate2D coordinate, Chunk chunk) {
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

    public static void unloadChunk(Coordinate2D coordinate) {
        Region r = regions.get(coordinate.chunkToRegion());
        if (r != null) {
            r.removeChunk(coordinate);
        }
    }

    /**
     * Loop to call save periodically.
     */
    @Override
    public void run() {
        setPriority(1);
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
    private static void save() {
        GuiManager.setSaving(true);
        if (!regions.isEmpty()) {
            // convert the values to an array first to prevent blocking any threads
            Region[] r = regions.values().toArray(new Region[regions.size()]);
            for (Region region : r) {
                McaFile file = region.toFile();
                if (file == null) { continue; }

                try {
                    file.write();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // save level.dat
        try {
            saveLevelData();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // remove empty regions
        regions.entrySet().removeIf(el -> el.getValue().isEmpty());

        GuiManager.setSaving(false);
    }
}
