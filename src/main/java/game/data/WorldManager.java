package game.data;

import game.Game;
import game.data.chunk.Chunk;
import game.data.chunk.palette.BlockColors;
import game.data.chunk.palette.GlobalPalette;
import game.data.region.McaFile;
import game.data.region.Region;
import gui.GuiManager;
import org.apache.commons.io.IOUtils;
import proxy.CompressionManager;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.DoubleTag;
import se.llbit.nbt.ErrorTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

    private static GlobalPalette globalPalette;

    private static BlockColors blockColors;

    private static boolean markNewChunks;

    private static boolean writeChunks;

    private WorldManager() {

    }

    /**
     * Read from the save path to see which chunks have been saved already.
     */
    public static void loadExistingChunks() throws IOException {
        Path exportDir = Paths.get(Game.getExportDirectory(), "region");

        List<Coordinate2D> existing = Files.walk(exportDir)
            .filter(el -> el.getFileName().toString().endsWith(".mca"))
            .map(Path::toFile)
            .limit(100) // don't load more than 100 region files
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

        System.out.println(existing.size());
        GuiManager.drawExistingChunks(existing);
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
        byte[] fileContent = IOUtils.toByteArray(fileInput);

        // get default level.dat
        Tag root = NamedTag.read(
            new DataInputStream(new ByteArrayInputStream(CompressionManager.gzipDecompress(fileContent)))
        );

        CompoundTag data = (CompoundTag) root.unpack().get("Data");

        // add the player's position
        Coordinate3D playerPosition = Game.getPlayerPosition();
        if (playerPosition != null) {
            Tag playerTag = data.get("Player");
            CompoundTag player;
            if (playerTag instanceof ErrorTag) {
                player = new CompoundTag();
            } else {
                player = (CompoundTag) playerTag;
                data.add("Player", player);
            }

            player.add("Pos", new ListTag(Tag.TAG_DOUBLE, Arrays.asList(
                new DoubleTag(playerPosition.getX() * 1.0),
                new DoubleTag(playerPosition.getY() * 1.0),
                new DoubleTag(playerPosition.getZ() * 1.0)
            )));

            // set the world spawn to match the last known player location
            data.add("SpawnX", new IntTag(playerPosition.getX()));
            data.add("SpawnY", new IntTag(playerPosition.getY()));
            data.add("SpawnZ", new IntTag(playerPosition.getZ()));
        }

        // add the seed & last played time
        data.add("RandomSeed", new LongTag(Game.getSeed()));
        data.add("LastPlayed", new LongTag(System.currentTimeMillis()));

        // add the version
        if (Game.getDataVersion() > 0 && Game.getGameVersion() != null) {
            CompoundTag versionTag = new CompoundTag();
            versionTag.add("Id", new IntTag(Game.getDataVersion()));
            versionTag.add("Name", new StringTag(Game.getGameVersion()));
            versionTag.add("Snapshot", new ByteTag((byte) 0));

            data.add("Version", versionTag);
        }

        // write the file
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        root.write(new DataOutputStream(output));

        byte[] compressed = CompressionManager.gzipCompress(output.toByteArray());
        Files.write(levelDat.toPath(), compressed);
    }

    /**
     * Start the periodic saving service.
     */
    public static void startSaveService(boolean markNewChunks, Boolean writeChunks) {
        WorldManager.markNewChunks = markNewChunks;

        if (writer != null) {
            return;
        }

        blockColors = BlockColors.create();

        writer = new WorldManager();
        writer.start();
    }

    /**
     * Add a parsed chunk to the correct region.
     * @param coordinate the chunk coordinates
     * @param chunk      the chunk
     */
    public static void loadChunk(Coordinate2D coordinate, Chunk chunk) {
        if (writeChunks) {
            Coordinate2D regionCoordinates = coordinate.chunkToRegion();

            if (!regions.containsKey(regionCoordinates)) {
                regions.put(regionCoordinates, new Region(regionCoordinates));
            }

            regions.get(regionCoordinates).addChunk(coordinate, chunk);
        }

        // draw the chunk once its been parsed
        chunk.whenParsed(() -> GuiManager.setChunkLoaded(coordinate, chunk));
    }

    /**
     * Get a chunk from the region its in.
     * @param coordinate the global chunk coordinates
     * @return the chunk
     */
    public static Chunk getChunk(Coordinate2D coordinate) {
        if (!regions.containsKey(coordinate.chunkToRegion())) {
            return null;
        }
        return regions.get(coordinate.chunkToRegion()).getChunk(coordinate);
    }

    public static void unloadChunk(Coordinate2D coordinate) {
        Region r = regions.get(coordinate.chunkToRegion());
        if (r != null) {
            r.removeChunk(coordinate);
        }
    }

    public static void setGlobalPalette(String version) {
        globalPalette = new GlobalPalette(version);
    }

    public static GlobalPalette getGlobalPalette() {
        return globalPalette;
    }

    public static BlockColors getBlockColors() {
        return blockColors;
    }

    public static boolean markNewChunks() {
        return markNewChunks;
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
        if (!writeChunks) {
            return;
        }
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
    }
}
