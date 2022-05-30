package game.data;

import static util.ExceptionHandling.attempt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import config.Config;
import game.data.chunk.BlockEntityRegistry;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import game.data.chunk.ChunkEntities;
import game.data.chunk.ChunkFactory;
import game.data.chunk.IncompleteChunkException;
import game.data.chunk.palette.BiomeRegistry;
import game.data.chunk.palette.BlockColors;
import game.data.chunk.palette.BlockState;
import game.data.container.ContainerManager;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionCodec;
import game.data.entity.EntityNames;
import game.data.entity.EntityRegistry;
import game.data.entity.specific.VillagerProfessionRegistry;
import game.data.entity.specific.VillagerTypeRegistry;
import game.data.maps.MapRegistry;
import game.data.region.McaFile;
import game.data.region.McaFilePair;
import game.data.region.Region;
import gui.GuiManager;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import util.PathUtils;

/**
 * Manage the world, including saving, parsing and updating the GUI.
 */
public class WorldManager {
    private static final int INIT_SAVE_DELAY = 5 * 1000;
    private static final int SAVE_DELAY = 12 * 1000;
    private static WorldManager instance;
    private final LevelData levelData;
    private final MapRegistry mapRegistry;
    private final Map<CoordinateDim2D, Queue<Runnable>> chunkLoadCallbacks = new ConcurrentHashMap<>();
    private Map<CoordinateDim2D, Region> regions = new ConcurrentHashMap<>();
    private final Set<Dimension> existingLoaded = new HashSet<>();

    private EntityNames entityMap;
    private MenuRegistry menuRegistry;
    private ItemRegistry itemRegistry;
    private BiomeRegistry biomeRegistry;
    private BlockColors blockColors;
    private BlockEntityRegistry blockEntityRegistry;
    private VillagerProfessionRegistry villagerProfessionRegistry;
    private VillagerTypeRegistry villagerTypeRegistry;

    private boolean markNewChunks;
    private boolean writeChunks;
    private boolean isStarted;
    private boolean isPaused;
    private boolean isSaving;

    private ContainerManager containerManager;
    private DimensionCodec dimensionCodec;
    private RenderDistanceExtender renderDistanceExtender;

    private BiConsumer<CoordinateDouble3D, Double> playerPosListener;
    private CoordinateDouble3D playerPosition;
    private double playerRotation = 0;
    private Dimension dimension;
    private final EntityRegistry entityRegistry;
    private final ChunkFactory chunkFactory;

    public WorldManager() {
        this.isStarted = false;
        this.entityMap = new EntityNames();
        this.entityRegistry = new EntityRegistry(this);
        this.chunkFactory = new ChunkFactory();
        this.mapRegistry = new MapRegistry();

        this.levelData = new LevelData(this);

        try {
            this.levelData.load();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        this.playerPosition = this.levelData.getPlayerPosition();
        this.dimension = this.levelData.getPlayerDimension();
    }

    public static WorldManager getInstance() {
        if (instance == null) {
            instance = new WorldManager();
        }
        return instance;
    }

    public static void setInstance(WorldManager worldManager) {
        instance = worldManager;
    }

    public void registerChunkLoadCallback(CoordinateDim2D coordinate, Runnable r) {
        chunkLoadCallbacks.putIfAbsent(coordinate, new ConcurrentLinkedQueue<>());
        chunkLoadCallbacks.get(coordinate).add(r);
    }

    public void deregisterChunkLoadCallback(CoordinateDim2D coordinate, Runnable r) {
        if (chunkLoadCallbacks.containsKey(coordinate)) {
            chunkLoadCallbacks.get(coordinate).remove(r);
        }
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.invalidateChunks();
        }
        unloadChunksNotIn(dimension);

        GuiManager.setDimension(this.dimension);
        outlineExistingChunks();
    }

    /**
     * Unload all chunks except those in the given dimension. Used when changing dimension or disconnecting.
     */
    private void unloadChunksNotIn(Dimension dimension) {
        for (Map.Entry<CoordinateDim2D, Region> r : this.regions.entrySet()) {
            if (!r.getKey().getDimension().equals(dimension)) {
                r.getValue().unloadAll();
            }
        }
    }

    public double getPlayerRotation() {
        return playerRotation;
    }

    public void setPlayerRotation(double playerRotation) {
        this.playerRotation = playerRotation;

        if (this.playerPosListener != null) {
            this.playerPosListener.accept(this.playerPosition, this.playerRotation);
        }
    }

    public void updateExtendedRenderDistance(int val) {
        if (val == 0 && this.renderDistanceExtender == null) {
            return;
        }

        if (val > 0 && this.renderDistanceExtender == null) {
            this.renderDistanceExtender = new RenderDistanceExtender(this, val);
        } else {
            this.renderDistanceExtender.setExtendedDistance(val);
        }
    }

    public void outlineExistingChunks() {
        int limit = 64000;
        if (existingLoaded.contains(this.dimension)) {
            return;
        }
        existingLoaded.add(this.dimension);

        Collection<McaFile> files = McaFile.getFiles(this.playerPosition.discretize().globalToChunk(), dimension, 32).collect(Collectors.toList());

        int total = 0;
        for (McaFile f : files) {
            if (total > limit) {
                break;
            }

            List<CoordinateDim2D> list = f.getChunkPositions(this.dimension);
            total += list.size();
            GuiManager.outlineExistingChunks(list);
        }

    }

    /**
     * Draw all previously-downloaded chunks in the GUI. We can't just load them all and immediately draw them to the
     * GUI, as the shading requires that we look at neighbouring chunks. We first add them all to the world manager,
     * then draw them, and then delete them. This is more work but ensures proper shading on all chunks.
     * @param center
     */
    public void drawExistingChunks(Coordinate2D center) {
        int limit = 48000;
        Collection<McaFile> files = McaFile.getFiles(center, this.dimension, 24).collect(Collectors.toList());

        int chunksLoaded = 0;
        for (McaFile file : files) {
            if (chunksLoaded > limit) {
                break;
            }
            Map<CoordinateDim2D, Chunk> chunks = file.getParsedChunks(this.dimension);

            // Step 2: add all chunks to the WorldManager if it doesn't have them yet
            Set<CoordinateDim2D> toDelete = new HashSet<>();
            for (Map.Entry<CoordinateDim2D, Chunk> entry : chunks.entrySet()) {
                if (chunksLoaded > limit) {
                    break;
                }

                CoordinateDim2D coord = entry.getKey();
                Chunk chunk = entry.getValue();
                if (getChunk(coord) == null) {
                    toDelete.add(coord);
                    loadChunk(chunk, false, false);
                    chunksLoaded++;
                }
            }

            // Step 3: draw to GUI
            chunks.forEach(GuiManager::setChunkLoaded);

            // Step 4: delete the newly added chunks
            toDelete.forEach(this::unloadChunk);

        }
    }

    /**
     * Set the config variables for the save service.
     */
    public void setWorldManagerVariables(boolean markNewChunks, Boolean writeChunks) {
        this.markNewChunks = markNewChunks;
        this.writeChunks = writeChunks;
    }

    /**
     * Start the periodic saving service.
     */
    public void startSaveService() {
        if (isStarted) {
            return;
        }
        isStarted = true;

        this.start();
    }

    /**
     * Add a parsed chunk to the correct region.
     *
     * @param chunk the chunk
     */
    public void loadChunk(Chunk chunk, boolean drawInGui, boolean overrideExisting) {
        if (!drawInGui || writeChunks) {
            CoordinateDim2D regionCoordinates = chunk.location.chunkToDimRegion();

            Region r = regions.computeIfAbsent(regionCoordinates, Region::new);

            r.addChunk(chunk.location, chunk, overrideExisting);
        }

        if (drawInGui) {
            // draw the chunk once its been parsed
            chunk.whenParsed(() -> GuiManager.setChunkLoaded(chunk.location, chunk));
        }

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.notifyLoaded(chunk.location);
        }
    }

    public void chunkLoadedCallback(CoordinateDim2D coordinateDim2D) {
        // run callbacks
        Queue<Runnable> callbacks = chunkLoadCallbacks.remove(coordinateDim2D);
        if (callbacks != null) {
            while (!callbacks.isEmpty()) {
                callbacks.remove().run();
            }
        }
    }

    /**
     * Get a chunk from the region its in.
     *
     * @param coordinate the global chunk coordinates
     * @return the chunk
     */
    public Chunk getChunk(CoordinateDim2D coordinate) {
        return regions.getOrDefault(coordinate.chunkToDimRegion(), Region.EMPTY).getChunk(coordinate);
    }

    public Chunk getChunk(Coordinate2D coords) {
        return getChunk(coords.addDimension(this.dimension));
    }

    public void unloadChunk(CoordinateDim2D coordinate) {
        chunkFactory.unloadChunk(coordinate);

        CoordinateDim2D regionCoordinate = coordinate.chunkToDimRegion();
        Region r = regions.get(regionCoordinate);
        if (r != null) {
            r.removeChunk(coordinate);

            if (r.canRemove()) {
                regions.remove(regionCoordinate);
            }
        }
        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.notifyUnloaded(coordinate);
        }
    }

    public BlockState blockStateAt(Coordinate3D coordinate3D) {
        Chunk c = this.getChunk(coordinate3D.globalToChunk().addDimension(this.dimension));

        if (c == null) {
            return null;
        }

        Coordinate3D pos = coordinate3D.withinChunk();
        return c.getBlockStateAt(pos);
    }

    public EntityNames getEntityMap() {
        return entityMap;
    }

    public void setEntityMap(EntityNames names) {
        entityMap = names;
    }

    public BlockColors getBlockColors() {
        return blockColors;
    }

    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    public boolean markNewChunks() {
        return markNewChunks;
    }

    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public void setMenuRegistry(MenuRegistry menus) {
        menuRegistry = menus;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public void setItemRegistry(ItemRegistry items) {
        itemRegistry = items;
    }
    
    public BlockEntityRegistry getBlockEntityMap() {
        return blockEntityRegistry;
    }

    public void setBlockEntityMap(BlockEntityRegistry blockEntities) {
        blockEntityRegistry = blockEntities;
    }

    public VillagerProfessionRegistry getVillagerProfessionMap() {
        return villagerProfessionRegistry;
    }

    public void setBlockEntityMap(VillagerProfessionRegistry villagerProfessions) {
        villagerProfessionRegistry = villagerProfessions;
    }
    
    public VillagerTypeRegistry getVillagerTypeMap() {
        return villagerTypeRegistry;
    }

    public void setBlockEntityMap(VillagerTypeRegistry villagerTypes) {
        villagerTypeRegistry = villagerTypes;
    }

    /**
     * Mark a chunk and it's region as having unsaved changes.
     */
    public void touchChunk(ChunkEntities c) {
        c.touch();
        regions.get(c.getLocation().chunkToDimRegion()).touch();
    }

    public DimensionCodec getDimensionCodec() {
        return dimensionCodec;
    }

    /**
     * Set the dimension codec, used to store information about the dimensions that this server supports.
     */
    public void setDimensionCodec(DimensionCodec codec) {
        dimensionCodec = codec;

        // We can immediately try to write the dimension data to the proper directory.
        try {
            Path p = PathUtils.toPath(Config.getWorldOutputDir(), "datapacks", "downloaded", "data");
            if (codec.write(p)) {

                // we need to copy that pack.mcmeta file from so that Minecraft will recognise the datapack
                Path packMeta = PathUtils.toPath(p.getParent().toString(), "pack.mcmeta");
                InputStream in = WorldManager.class.getClassLoader().getResourceAsStream("pack.mcmeta");
                byte[] bytes = IOUtils.toByteArray(in);
                Files.write(packMeta, bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not write dimension codec. Custom dimensions may not work properly.");
        }
    }

    /**
     * Periodically save the world.
     */
    public void start() {
        ThreadFactory namedThreadFactory = r -> new Thread(r, "World Save Service");
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, namedThreadFactory);
        executor.scheduleWithFixedDelay(() -> attempt(this::save), INIT_SAVE_DELAY, SAVE_DELAY, TimeUnit.MILLISECONDS);
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
                McaFilePair files = region.toFile(getPlayerPosition().globalToChunk());
                if (files == null) {
                    continue;
                }

                write(files.getRegion());
                write(files.getEntities());
            }
        }

        // save level.dat
        try {
            levelData.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // save map data
        try {
            mapRegistry.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // remove empty regions
        regions.entrySet().removeIf(el -> el.getValue().isEmpty());

        isSaving = false;

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.checkDistance();
        }

        // suggest GC to clear up some memory that may have been freed by saving
        System.gc();
    }

    private void write(McaFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        try {
            file.write();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ContainerManager getContainerManager() {
        if (containerManager == null) {
            containerManager = new ContainerManager();
        }
        return containerManager;
    }

    public void pauseSaving() {
        isPaused = true;
        System.out.println("Pausing");
    }

    public void resumeSaving() {
        isPaused = false;
        System.out.println("Resuming");
    }

    public void deleteAllExisting() {
        regions = new HashMap<>();
        chunkFactory.clear();

        try {
            File dir = PathUtils.toPath(Config.getWorldOutputDir(), this.dimension.getPath(), "region").toFile();

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

    public void setPlayerPosListener(BiConsumer<CoordinateDouble3D, Double> playerPosListener) {
        this.playerPosListener = playerPosListener;
    }

    public Coordinate3D getPlayerPosition() {
        return playerPosition.discretize();
    }

    public void setPlayerPosition(CoordinateDouble3D newPos) {
        this.playerPosition = newPos;

        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.updatePlayerPos(getPlayerPosition());
        }
        if (this.playerPosListener != null) {
            this.playerPosListener.accept(newPos, this.playerRotation);
        }
    }

    public void setServerRenderDistance(int viewDist) {
        if (renderDistanceExtender != null) {
            renderDistanceExtender.setServerReportedRenderDistance(viewDist);
        }

    }

    /**
     * Send unload chunk packets to the client for each of the coordinates. Currently not used as chunk unloading is
     * not really important, the client can figure it out.
     *
     * @param toUnload the set of chunks to unload.
     */
    public void unloadChunks(Collection<Coordinate2D> toUnload) {
        for (Coordinate2D coords : toUnload) {
            unloadChunk(coords.addDimension(getDimension()));
        }
    }

    /**
     * Load chunks from their MCA files and send them to the client.
     *
     * @param desired the set of chunk coordinates which we want to send to the client.
     * @return the set of chunks that was actually sent to the client.
     */
    public Set<Coordinate2D> loadChunks(Collection<Coordinate2D> desired) {
        Set<Coordinate2D> loaded = new HashSet<>();

        int chunksSent = 0;
        Map<Coordinate2D, McaFile> loadedFiles = new HashMap<>();
        for (Coordinate2D coords : desired) {
            // since there is delay in this loop, it's possible some of the chunks were sent to the client by the time
            // we get to them.
            if (this.renderDistanceExtender.isLoaded(coords)) {
                continue;
            }

            McaFile mca = loadedFiles.computeIfAbsent(coords.chunkToRegion(), (c) -> McaFile.ofCoords(c.addDimension(this.dimension)));

            if (mca == null) {
                continue;
            }

            CoordinateDim2D withDim = coords.addDimension(this.dimension);
            ChunkBinary chunkBinary = mca.getChunkBinary(withDim);

            // skip any chunks not in the MCA file
            if (chunkBinary == null) {
                continue;
            }

            // send a packet with the chunk to the client
            Chunk chunk = chunkBinary.toChunk(withDim);

            try {
                PacketBuilder chunkData = chunk.toPacket();
                PacketBuilder light = chunk.toLightPacket();
                if (light != null) {
                    Config.getPacketInjector().accept(light);
                }
                Config.getPacketInjector().accept(chunkData);

            } catch (IncompleteChunkException ex) {
                ex.printStackTrace();
                // chunk was not complete
                continue;
            }
            loaded.add(coords);

            // draw in GUI
            loadChunk(chunk, true, false);

            // periodically sleep so the client doesn't stutter from receiving too many chunks
            chunksSent = (chunksSent + 1) % 5;
            if (chunksSent == 0) {
                try {
                    Thread.sleep(48);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        return loaded;
    }

    public void resetConnection() {
        if (this.renderDistanceExtender != null) {
            this.renderDistanceExtender.resetConnection();
        }
        this.entityRegistry.reset();
        this.chunkFactory.reset();
        this.unloadChunksNotIn(null);
    }

    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }
    public ChunkFactory getChunkFactory() {
        return chunkFactory;
    }
    public MapRegistry getMapRegistry() {
        return mapRegistry;
    }

    public void unloadEntities(CoordinateDim2D coord) {
        this.entityRegistry.unloadChunk(coord);
        this.chunkFactory.unloadChunk(coord);
    }

    public int countActiveChunks() {
        int total = 0;
        for (Region r : regions.values()) {
            total += r.countChunks();
        }
        return total;
    }

    public void blockChange(DataTypeProvider provider) {
        if (!Config.handleBlockChanges()) {
            return;
        }
        chunkFactory.runOnFactoryThread(() -> {
            Coordinate3D coords = provider.readCoordinates();
            Chunk c = getChunk(coords.globalToChunk().addDimension(this.dimension));
            if (c == null) {
                return;
            }

            c.updateBlock(coords.globalToChunkLocal(), provider.readVarInt());
            touchChunk(c);
        });
    }

    public void multiBlockChange(Coordinate3D pos, DataTypeProvider provider) {
        if (!Config.handleBlockChanges()) {
            return;
        }
        chunkFactory.runOnFactoryThread(() -> {
            Chunk c = getChunk(pos.addDimension(this.dimension));
            if (c == null) {
                return;
            }
            c.updateBlocks(pos, provider);
        });

    }


    /**
     * Update a chunk with lighting data. If the chunk is not known yet, give it to the chunk factory. If it is known,
     * it is given to the chunk to parse immediately.
     */
    public void updateLight(DataTypeProvider provider) {
        chunkFactory.runOnFactoryThread(() -> {
            int chunkX = provider.readVarInt();
            int chunkZ = provider.readVarInt();
            CoordinateDim2D coords = new CoordinateDim2D(chunkX, chunkZ, dimension);
            Chunk c = getChunk(coords);
            if (c == null) {
                chunkFactory.updateLight(coords, provider);
            } else {
                c.updateLight(provider);
                touchChunk(c);

                GuiManager.setChunkState(coords, c.getState());
            }
        });

    }

    public int countActiveRegions() {
        return this.regions.size();
    }

    public int countActiveBinaryChunks() {
        return this.regions.values().stream().mapToInt(el -> el.getFile().countChunks()).sum();
    }

    public void initialiseRegistries() {
        blockColors = BlockColors.create();
        if (Config.versionReporter().isAtLeast1_18()) {
            biomeRegistry = BiomeRegistry.create();
        }
    }
}

