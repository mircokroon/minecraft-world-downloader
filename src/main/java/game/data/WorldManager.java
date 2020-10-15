package game.data;

import game.Game;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;
import game.data.chunk.entity.EntityNames;
import game.data.chunk.palette.BlockColors;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPalette;
import game.data.container.ContainerManager;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import game.data.region.McaFile;
import game.data.region.Region;
import gui.GuiManager;
import org.apache.commons.io.FileUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manage the world, including saving, parsing and updating the GUI.
 */
public class WorldManager extends Thread {
    private final static int SAVE_DELAY = 20 * 1000;

    private static Map<CoordinateDim2D, Region> regions = new ConcurrentHashMap<>();

    private static WorldManager writer = null;

    private static GlobalPalette globalPalette;
    private static EntityNames entityMap;
    private static MenuRegistry menuRegistry;
    private static ItemRegistry itemRegistry;

    private static BlockColors blockColors;

    private static boolean markNewChunks;

    private static boolean writeChunks;

    private static boolean isPaused;

    private static boolean isSaving;

    private static ContainerManager containerManager;

    private WorldManager() {

    }

    public static void outlineExistingChunks() throws IOException {
        Dimension dimension = Game.getDimension();
        Stream<McaFile> files = getMcaFiles(dimension, true);

        GuiManager.drawExistingChunks(
            files.flatMap(el -> el.getChunkPositions(dimension).stream()).collect(Collectors.toList())
        );
    }

    public static void drawExistingChunks() throws IOException {
        Dimension dimension = Game.getDimension();
        Stream<McaFile> files = getMcaFiles(dimension, false);

        // Step 1: parse all the chunks
        Set<Map.Entry<CoordinateDim2D, Chunk>> parsedChunks = files.parallel()
            .flatMap(el -> el.getParsedChunks(dimension).entrySet().stream())
            .collect(Collectors.toSet());

        // Step 2: add all chunks to the WorldManager if it doesn't have them yet
        Set<CoordinateDim2D> toDelete = new HashSet<>();
        parsedChunks.forEach(entry -> {
            if (getChunk(entry.getKey()) == null) {
                toDelete.add(entry.getKey());
                loadChunk(entry.getValue(), false);
            }
        });

        // Step 3: draw the picture
        parsedChunks.forEach(entry -> GuiManager.setChunkLoaded(entry.getKey(), entry.getValue()));

        // Step 4: delete the newly added chunks
        toDelete.forEach(WorldManager::unloadChunk);
    }

    /**
     * Read from the save path to see which chunks have been saved already.
     */
    private static Stream<McaFile> getMcaFiles(Dimension dimension, boolean limit) throws IOException {
        Path exportDir = Paths.get(Game.getExportDirectory(), dimension.getPath(), "region");

        Stream<File> stream = Files.walk(exportDir)
            .filter(el -> el.getFileName().toString().endsWith(".mca"))
            .map(Path::toFile);

        if (limit) {
            stream = stream.limit(100); // don't load more than 100 region files
        }

        return stream.filter(el -> el.length() > 0)
            .map(el -> {
                try {
                    return new McaFile(el);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .filter(Objects::nonNull);
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

        if (!Game.isWorldGenEnabled()) {
            disableWorldGeneration(data);
        }

        // write the file
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        root.write(new DataOutputStream(output));

        byte[] compressed = CompressionManager.gzipCompress(output.toByteArray());
        Files.write(levelDat.toPath(), compressed);
    }

    /**
     * Set world type to a superflat void world.
     */
    private static void disableWorldGeneration(CompoundTag data) {
        data.add("generatorName", new StringTag("flat"));

        // this is the 1.12.2 superflat format, but it still works in later versions.
        data.add("generatorOptions", new StringTag("3;minecraft:air;127"));
    }

    /**
     * Set the config variables for the save service.
     */
    public static void setSaveServiceVariables(boolean markNewChunks, Boolean writeChunks) {
        WorldManager.markNewChunks = markNewChunks;
        WorldManager.writeChunks = writeChunks;

        blockColors = BlockColors.create();
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

        ChunkFactory.getInstance().parseEntities();
    }

    /**
     * Add a parsed chunk to the correct region.
     * @param chunk      the chunk
     */
    public static void loadChunk(Chunk chunk, boolean drawInGui) {
        if (!drawInGui || writeChunks) {
            CoordinateDim2D regionCoordinates = chunk.location.chunkToDimRegion();

            if (!regions.containsKey(regionCoordinates)) {
                regions.put(regionCoordinates, new Region(regionCoordinates));
            }

            regions.get(regionCoordinates).addChunk(chunk.location, chunk);
        }

        if (drawInGui) {
            // draw the chunk once its been parsed
            chunk.whenParsed(() -> GuiManager.setChunkLoaded(chunk.location, chunk));
        }
    }

    /**
     * Get a chunk from the region its in.
     * @param coordinate the global chunk coordinates
     * @return the chunk
     */
    public static Chunk getChunk(CoordinateDim2D coordinate) {
        if (!regions.containsKey(coordinate.chunkToDimRegion())) {
            return null;
        }
        return regions.get(coordinate.chunkToDimRegion()).getChunk(coordinate);
    }

    public static void unloadChunk(CoordinateDim2D coordinate) {
        Region r = regions.get(coordinate.chunkToDimRegion());
        if (r != null) {
            r.removeChunk(coordinate);
        }
    }

    public static BlockState blockStateAt(Coordinate3D coordinate3D) {
        Chunk c = WorldManager.getChunk(coordinate3D.globalToChunk().addDimension(Game.getDimension()));

        if (c == null) { return null; }

        Coordinate3D pos = coordinate3D.withinChunk();
        return c.getBlockStateAt(pos);
    }

    public static void setGlobalPalette(GlobalPalette palette) {
        globalPalette = palette;
    }

    public static void setEntityMap(EntityNames names) {
        entityMap = names;
    }

    public static void setMenuRegistry(MenuRegistry menus) {
        menuRegistry = menus;
    }

    public static GlobalPalette getGlobalPalette() {
        return globalPalette;
    }
    public static EntityNames getEntityMap() {
        return entityMap;
    }

    public static BlockColors getBlockColors() {
        return blockColors;
    }

    public static boolean markNewChunks() {
        return markNewChunks;
    }

    public static MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public static ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public static void setItemRegistry(ItemRegistry items) {
        itemRegistry = items;
    }

    /**
     * Mark a chunk and it's region as having unsaved changes.
     */
    public static void touchChunk(Chunk c) {
        c.touch();
        regions.get(c.location.chunkToDimRegion()).touch();
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
    public static void save() {
        if (!writeChunks) {
            return;
        }

        // make sure we can't have two saving calls at once (due to save & exit)
        if (isSaving) {
            return;
        }
        isSaving = true;


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

        isSaving = false;
    }

    public static ContainerManager getContainerManager() {
        if (containerManager == null) {
            containerManager = new ContainerManager();
        }
        return containerManager;
    }

    public static void pauseSaving() {
        isPaused = true;
    }

    public static void resumeSaving() {
        isPaused = false;
    }

    public static void deleteAllExisting() {
        regions = new HashMap<>();
        ChunkFactory.getInstance().clear();

        try {
            File dir = Paths.get(Game.getExportDirectory(), Game.getDimension().getPath(), "region").toFile();

            if (dir.isDirectory()) {
                FileUtils.cleanDirectory(dir);
            }
        } catch (IOException ex) {
            System.out.println("Could not delete region files. Reason: " + ex.getMessage());
        }

        GuiManager.clearChunks();
    }

    public static boolean isPaused() {
        return isPaused;
    }


}

