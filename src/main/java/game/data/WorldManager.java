package game.data;

import game.Config;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import game.data.chunk.ChunkFactory;
import game.data.chunk.entity.EntityNames;
import game.data.chunk.palette.BlockColors;
import game.data.chunk.palette.BlockState;
import game.data.chunk.version.Chunk_1_14;
import game.data.container.ContainerManager;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionCodec;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static util.ExceptionHandling.attempt;

/**
 * Manage the world, including saving, parsing and updating the GUI.
 */
public class WorldManager {
    private static final int SAVE_DELAY = 20 * 1000;

    private Map<CoordinateDim2D, Region> regions = new ConcurrentHashMap<>();

    private EntityNames entityMap;
    private MenuRegistry menuRegistry;
    private ItemRegistry itemRegistry;

    private BlockColors blockColors;

    private boolean markNewChunks;

    private boolean writeChunks;

    private boolean isStarted;

    private boolean isPaused;

    private boolean isSaving;

    private ContainerManager containerManager;

    private DimensionCodec dimensionCodec;

    private RenderDistanceExtender renderDistanceExtender;
    private Coordinate3D playerPosition = new Coordinate3D(0, 80, 0);
    private double playerRotation = 0;

    private Dimension dimension = Dimension.OVERWORLD;

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.invalidateChunks();
        }
    }

    public double getPlayerRotation() {
        return playerRotation;
    }

    public void setPlayerRotation(double playerRotation) {
        this.playerRotation = playerRotation;
    }

    protected WorldManager() {
        this.isStarted = false;

        if (Config.getExtendedRenderDistance() > 0) {
            this.renderDistanceExtender = new RenderDistanceExtender(this, Config.getExtendedRenderDistance());
        }
    }

    public RenderDistanceExtender getRenderDistanceExtender() {
        return renderDistanceExtender;
    }

    private static WorldManager instance;

    public static WorldManager getInstance() {
        if (instance == null) {
            if (Config.getDataVersion() < Chunk_1_14.DATA_VERSION && Config.getExtendedRenderDistance() > 0) {
                instance = new WorldManager_1_13();
            } else {
                instance = new WorldManager();
            }
        }
        return instance;
    }

    public static void setInstance(WorldManager worldManager) {
        instance = worldManager;
    }


    /**
     * Set the dimension codec, used to store information about the dimensions that this server supports.
     */
    public void setDimensionCodec(DimensionCodec codec) {
        dimensionCodec = codec;

        // We can immediately try to write the dimension data to the proper directory.
        try {
            Path p = Paths.get(Config.getExportDirectory(), "datapacks", "downloaded", "data");
            if (codec.write(p)) {

                // we need to copy that pack.mcmeta file from so that Minecraft will recognise the datapack
                Path packMeta = Paths.get(p.getParent().toString(), "pack.mcmeta");
                InputStream in = WorldManager.class.getClassLoader().getResourceAsStream("pack.mcmeta");
                byte[] bytes = IOUtils.toByteArray(in);
                Files.write(packMeta, bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not write dimension codec. Custom dimensions may not work properly.");
        }
    }

    public void outlineExistingChunks() throws IOException {
        Stream<McaFile> files = getMcaFiles(dimension, true);

        GuiManager.outlineExistingChunks(
                files.flatMap(el -> el.getChunkPositions(this.dimension).stream()).collect(Collectors.toList())
        );
    }

    /**
     * Draw all previously-downloaded chunks in the GUI. We can't just load them all and immediately draw them to the
     * GUI, as the shading requires that we look at neighbouring chunks. We first add them all to the world manager,
     * then draw them, and then delete them. This is more work but ensures proper shading on all chunks.
     */
    public void drawExistingChunks() throws IOException {
        Stream<McaFile> files = getMcaFiles(dimension, false);

        // Step 1: parse all the chunks
        Set<Map.Entry<CoordinateDim2D, Chunk>> parsedChunks = files.parallel()
                .flatMap(el -> el.getParsedChunks(this.dimension).entrySet().stream())
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
        toDelete.forEach(this::unloadChunk);
    }

    /**
     * Read from the save path to see which chunks have been saved already.
     */
    private Stream<McaFile> getMcaFiles(Dimension dimension, boolean limit) throws IOException {
        Path exportDir = Paths.get(Config.getExportDirectory(), dimension.getPath(), "region");

        if (!exportDir.toFile().exists()) {
            return Stream.empty();
        }

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
    private void saveLevelData() throws IOException {
        // make sure the folder exists
        File directory = Paths.get(Config.getExportDirectory()).toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File levelDat = Paths.get(Config.getExportDirectory(), "level.dat").toFile();

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
        data.add("RandomSeed", new LongTag(Config.getSeed()));
        data.add("LastPlayed", new LongTag(System.currentTimeMillis()));

        // add the version
        if (Config.getDataVersion() > 0 && Config.getGameVersion() != null) {
            CompoundTag versionTag = new CompoundTag();
            versionTag.add("Id", new IntTag(Config.getDataVersion()));
            versionTag.add("Name", new StringTag(Config.getGameVersion()));
            versionTag.add("Snapshot", new ByteTag((byte) 0));

            data.add("Version", versionTag);
        }

        if (!Config.isWorldGenEnabled()) {
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
    private void disableWorldGeneration(CompoundTag data) {
        data.add("generatorName", new StringTag("flat"));

        // this is the 1.12.2 superflat format, but it still works in later versions.
        data.add("generatorOptions", new StringTag("3;minecraft:air;127"));
    }

    /**
     * Set the config variables for the save service.
     */
    public void setSaveServiceVariables(boolean markNewChunks, Boolean writeChunks) {
        this.markNewChunks = markNewChunks;
        this.writeChunks = writeChunks;

        blockColors = BlockColors.create();
    }

    /**
     * Start the periodic saving service.
     */
    public void startSaveService() {
        if (isStarted) {
            return;
        }

        instance.start();

        ChunkFactory.getInstance().parseEntities();
    }

    /**
     * Add a parsed chunk to the correct region.
     * @param chunk      the chunk
     */
    public void loadChunk(Chunk chunk, boolean drawInGui) {
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
    public Chunk getChunk(CoordinateDim2D coordinate) {
        if (!regions.containsKey(coordinate.chunkToDimRegion())) {
            return null;
        }
        return regions.get(coordinate.chunkToDimRegion()).getChunk(coordinate);
    }

    public void unloadChunk(CoordinateDim2D coordinate) {
        Region r = regions.get(coordinate.chunkToDimRegion());
        if (r != null) {
            r.removeChunk(coordinate);
        }
    }

    public BlockState blockStateAt(Coordinate3D coordinate3D) {
        Chunk c = this.getChunk(coordinate3D.globalToChunk().addDimension(this.dimension));

        if (c == null) { return null; }

        Coordinate3D pos = coordinate3D.withinChunk();
        return c.getBlockStateAt(pos);
    }

    public void setEntityMap(EntityNames names) {
        entityMap = names;
    }

    public void setMenuRegistry(MenuRegistry menus) {
        menuRegistry = menus;
    }

    public EntityNames getEntityMap() {
        return entityMap;
    }

    public BlockColors getBlockColors() {
        return blockColors;
    }

    public boolean markNewChunks() {
        return markNewChunks;
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public void setItemRegistry(ItemRegistry items) {
        itemRegistry = items;
    }

    /**
     * Mark a chunk and it's region as having unsaved changes.
     */
    public void touchChunk(Chunk c) {
        c.touch();
        regions.get(c.location.chunkToDimRegion()).touch();
    }

    public DimensionCodec getDimensionCodec() {
        return dimensionCodec;
    }

    /**
     * Periodically save the world.
     */
    public void start() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleWithFixedDelay(this::save, 5000, SAVE_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Save the world. Will tell all regions to save their chunks.
     */
    public void save() {
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
            Region[] r = regions.values().toArray(new Region[0]);
            for (Region region : r) {
                McaFile file = region.toFile(playerPosition);
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

    public ContainerManager getContainerManager() {
        if (containerManager == null) {
            containerManager = new ContainerManager();
        }
        return containerManager;
    }

    public void pauseSaving() {
        isPaused = true;
    }

    public void resumeSaving() {
        isPaused = false;
    }

    public void deleteAllExisting() {
        regions = new HashMap<>();
        ChunkFactory.getInstance().clear();

        try {
            File dir = Paths.get(Config.getExportDirectory(), this.dimension.getPath(), "region").toFile();

            if (dir.isDirectory()) {
                FileUtils.cleanDirectory(dir);
            }
        } catch (IOException ex) {
            System.out.println("Could not delete region files. Reason: " + ex.getMessage());
        }

        GuiManager.clearChunks();
    }

    public boolean isPaused() {
        return isPaused;
    }


    public void setPlayerPosition(Coordinate3D newPos) {
        this.playerPosition = newPos;

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.updatePlayerPos(newPos);
        }
    }

    public Coordinate3D getPlayerPosition() {
        return playerPosition;
    }


    public void unloadChunks(Collection<Coordinate2D> toUnload) {
        // TODO
    }

    public Set<Coordinate2D> loadChunks(Collection<Coordinate2D> desired) {
        Set<Coordinate2D> loaded = new HashSet<>();

        // separate into McaFiles
        Map<Coordinate2D, List<Coordinate2D>> mcaFiles = desired.stream().collect(Collectors.groupingBy(Coordinate2D::chunkToRegion));

        // we need to avoid overwhelming the client with tons of chunks all at once, so we insert a small delay every
        // few chunks to avoid this.
        int chunksSent = 0;
        for (Map.Entry<Coordinate2D, List<Coordinate2D>> entry : mcaFiles.entrySet()) {
            Coordinate2D key = entry.getKey();
            List<Coordinate2D> value = entry.getValue();

            String filename = "r." + key.getX() + "." + key.getZ() + ".mca";
            File f = Paths.get(Config.getExportDirectory(), this.dimension.getPath(), "region", filename).toFile();

            if (!f.exists()) {
                continue;
            }

            // Load the MCA file - if it cannot be loaded for any reason it's skipped.
            McaFile m;
            try {
                m = new McaFile(f);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // loop through the list of chunks we want to load from this file
            for (Coordinate2D coord : value) {
                CoordinateDim2D withDim = coord.addDimension(this.dimension);
                ChunkBinary chunkBinary = m.getChunkBinary(withDim);

                // skip any chunks not in the MCA file
                if (chunkBinary == null) {
                    continue;
                }

                // send a packet with the chunk to the client
                Chunk chunk = chunkBinary.toChunk(withDim);
                Config.getPacketInjector().accept(chunk.toPacket());
                loaded.add(coord);

                // draw in GUI
                GuiManager.setChunkLoaded(chunk.location, chunk);

                // periodically sleep so the client doesn't stutter from receiving too many chunks
                chunksSent = (chunksSent + 1) % 5;
                if (chunksSent == 0) {
                    try {
                        Thread.sleep(24);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return loaded;
    }
}

