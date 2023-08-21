package gui.images;

import static util.ExceptionHandling.attempt;
import static util.ExceptionHandling.attemptQuiet;

import config.Config;
import game.data.WorldManager;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import gui.Bounds;
import gui.ChunkImageState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.apache.commons.io.FileUtils;

/**
 * Class to manage overlay images.
 */
public class RegionImageHandler {
    private static final String CACHE_PATH = "image-cache";
    private Map<Coordinate2D, RegionImages> regions;
    private Dimension activeDimension;
    private boolean isSaving = false;

    private final ScheduledExecutorService saveService;
    private static ImageMode overrideMode;
    private ImageMode imageMode = ImageMode.NORMAL;


    public RegionImageHandler() {
        this.regions = new ConcurrentHashMap<>();

        // TODO: remove or compress overview images far away to reduce memory usage
        saveService = Executors.newSingleThreadScheduledExecutor(
            (r) -> new Thread(r, "Region Image Handler")
        );
        saveService.scheduleWithFixedDelay(this::save, 20, 20, TimeUnit.SECONDS);
    }

    public static ImageMode getOverrideMode() {
        return overrideMode;
    }

    public static void setOverrideMode(ImageMode imageMode) {
        overrideMode = imageMode;
    }

    public void clear() {
        unload();
        attemptQuiet(() -> FileUtils.deleteDirectory(Paths.get(Config.getWorldOutputDir(), CACHE_PATH).toFile()));
    }

    public void drawChunk(CoordinateDim2D coordinate, Map<ImageMode, Image> imageMap, Boolean isSaved) {
        if (!coordinate.getDimension().equals(activeDimension)) {
            return;
        }

        Coordinate2D region = coordinate.chunkToRegion();

        RegionImages images = regions.computeIfAbsent(region, (coordinate2D -> loadRegion(coordinate)));

        Coordinate2D local = coordinate.toRegionLocal();

        imageMap.forEach((mode, image) -> {
            images.getImage(mode).drawChunk(local, image);
        });

        setChunkState(images, local, ChunkImageState.isSaved(isSaved));
    }

    public void setChunkState(Coordinate2D coords, ChunkImageState state) {
        Coordinate2D region = coords.chunkToRegion();

        RegionImages images = regions.get(region);
        if (images == null) {
            return;
        }

        setChunkState(images, coords.toRegionLocal(), state);
    }

    private void setChunkState(RegionImages image, Coordinate2D local, ChunkImageState state) {
        image.colourChunk(local, state.getColor());
    }


    public void markChunkSaved(CoordinateDim2D coordinate) {
        setChunkState(coordinate, ChunkImageState.SAVED);
    }

    private RegionImages loadRegion(Coordinate2D coordinate) {
        return RegionImages.of(activeDimension, coordinate);
    }

    private void save(Map<Coordinate2D, RegionImages> regions, Dimension dim) {
        // if shutdown is called, wait for saving to complete
        if (isSaving) {
            if (saveService != null) {
                attempt(() -> saveService.awaitTermination(10, TimeUnit.SECONDS));
            }
            return;
        }
        isSaving = true;

        for (ImageMode mode : ImageMode.values()) {
            attempt(() -> Files.createDirectories(dimensionPath(dim, mode)));
        }

        regions.forEach((coordinate, image) -> {
            attempt(() -> image.save(dim, coordinate));
        });

        isSaving = false;
    }

    public void save() {
        save(this.regions, this.activeDimension);
    }

    private void unload() {
        this.regions = new ConcurrentHashMap<>();
    }

    /**
     * Searches for all region files in a directory to load them in.
     */
    private void load() {
        Map<Coordinate2D, RegionImages> regionMap = regions;

        new Thread(() -> attemptQuiet(() -> {
            for (ImageMode mode : ImageMode.values()) {
                Files.walk(dimensionPath(this.activeDimension, mode), 1)
                    .limit(3200)
                    .forEach(image -> attempt(() -> load(regionMap, mode, image)));
            }
        })).start();
    }

    private void load(Map<Coordinate2D, RegionImages> regionMap, ImageMode mode, Path image) {
        if (!image.toString().toLowerCase().endsWith("png")) {
            return;
        }

        String[] parts = image.getFileName().toString().split("\\.");

        int x = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        Coordinate2D regionCoordinate = new Coordinate2D(x, z);

        regionMap.computeIfAbsent(regionCoordinate, k -> new RegionImages()).set(mode, image);
    }

    public void setDimension(Dimension dimension) {
        if (this.activeDimension == dimension) {
            return;
        }

        if (this.activeDimension != null) {
            save();
            unload();
        }

        this.activeDimension = dimension;
        load();
    }

    static Path dimensionPath(Dimension dim) {
        return Paths.get(Config.getWorldOutputDir(), CACHE_PATH, dim.getPath());
    }

    static Path dimensionPath(Dimension dim, ImageMode mode) {
        return Paths.get(Config.getWorldOutputDir(), CACHE_PATH, mode.path(), dim.getPath());
    }

    private void updateRenderMode() {
        boolean isNether = WorldManager.getInstance().getDimension().isNether();

        if (overrideMode != null) {
            imageMode = isNether ? overrideMode.other() : overrideMode;
            return;
        }

        if (Config.enableCaveRenderMode()) {
            imageMode = WorldManager.getInstance().isBelowGround() ? ImageMode.CAVES : ImageMode.NORMAL;
        } else {
            imageMode = isNether ? ImageMode.CAVES : ImageMode.NORMAL;
        }
    }

    public void drawAll(Bounds bounds, BiConsumer<Coordinate2D, Image> drawRegion) {
        updateRenderMode();

        regions.forEach((coordinate, images) -> {
            if (bounds.overlaps(coordinate)) {
                RegionImage image = images.getImage(imageMode);
                if (image == null) { return; }

                drawRegion.accept(coordinate, image.getImage());
                drawRegion.accept(coordinate, image.getChunkOverlay());
            }
        });
    }

    public int size() {
        return regions.size();
    }

    public void shutdown() {
        if (saveService != null) {
            saveService.shutdown();
        }
    }

    public void resetRegion(Coordinate2D region) {
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.NORMAL), region)));
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.CAVES), region)));
        regions.remove(region);
    }
}

class RegionImages {
    RegionImage normal;
    RegionImage caves;

    public RegionImages(RegionImage normal, RegionImage caves) {
        this.normal = normal;
        this.caves = caves;
    }

    public RegionImages() {
        normal = new RegionImage();
        caves = new RegionImage();
    }

    public static RegionImages of(Dimension dimension, Coordinate2D coordinate) {
        RegionImage normal = RegionImage.of(RegionImageHandler.dimensionPath(dimension, ImageMode.NORMAL).toFile());
        RegionImage caves = RegionImage.of(RegionImageHandler.dimensionPath(dimension, ImageMode.CAVES).toFile());

        return new RegionImages(normal, caves);
    }

    public RegionImage getImage(ImageMode mode) {
        return switch (mode) {
            case NORMAL -> normal;
            case CAVES -> caves;
        };
    }

    public void colourChunk(Coordinate2D local, Color color) {
        normal.colourChunk(local, color);
        if (caves != null) {
            caves.colourChunk(local, color);
        }
    }

    public void save(Dimension dim, Coordinate2D coordinate) throws IOException {
        normal.save(RegionImageHandler.dimensionPath(dim, ImageMode.NORMAL), coordinate);
        caves.save(RegionImageHandler.dimensionPath(dim, ImageMode.CAVES), coordinate);
    }

    public void set(ImageMode mode, Path image) {
        switch (mode) {
            case NORMAL -> normal = RegionImage.of(image.toFile());
            case CAVES -> caves = RegionImage.of(image.toFile());
        };
    }
}

