package gui.images;

import static gui.images.RegionImage.NORMAL_PREFIX;
import static gui.images.RegionImage.SMALL_PREFIX;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
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

    private final ScheduledExecutorService imageHandlerExecutor;
    private static ImageMode overrideMode;
    private ImageMode imageMode = ImageMode.NORMAL;

    ConcurrentLinkedQueue<RegionImages> resizeLater;


    public RegionImageHandler() {
        this.regions = new ConcurrentHashMap<>();
        this.resizeLater = new ConcurrentLinkedQueue<>();

        imageHandlerExecutor = Executors.newSingleThreadScheduledExecutor(
            (r) -> new Thread(r, "Region Image Handler")
        );
        imageHandlerExecutor.scheduleWithFixedDelay(this::save, 20, 20, TimeUnit.SECONDS);
        imageHandlerExecutor.scheduleWithFixedDelay(this::resizeLater, 15, 5, TimeUnit.SECONDS);
    }

    private void resizeLater() {
        while (!resizeLater.isEmpty()) {
            resizeLater.remove().allowResample();
        }
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
        RegionImages images = regions.computeIfAbsent(region, (coordinate2D -> loadRegion(region)));

        Coordinate2D local = coordinate.toRegionLocal();

        imageMap.forEach((mode, image) -> {
            boolean shouldResize = images.getImage(mode).drawChunk(local, image);

            if (shouldResize) {
                imageHandlerExecutor.schedule(() -> images.getImage(mode).allowResample(), 500, TimeUnit.MILLISECONDS);
            }
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

    private void allowResample(Map<Coordinate2D, RegionImages> regions, Dimension activeDimension) {
        regions.forEach((coordinate, image) -> {
            attempt(() -> image.allowResample());
        });
    }

    private void save(Map<Coordinate2D, RegionImages> regions, Dimension dim) {
        // if shutdown is called, wait for saving to complete
        if (isSaving) {
            if (imageHandlerExecutor != null) {
                attempt(() -> imageHandlerExecutor.awaitTermination(10, TimeUnit.SECONDS));
            }
            return;
        }
        isSaving = true;

        for (ImageMode mode : ImageMode.values()) {
            attempt(() -> Files.createDirectories(dimensionPath(dim, mode)));
        }

        regions.forEach((coordinate, image) -> {
            attempt(image::save);
        });

        isSaving = false;
    }

    public void save() {
        save(this.regions, this.activeDimension);
    }

    public void allowResample() {
        allowResample(this.regions, this.activeDimension);
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
        if (!image.toString().toLowerCase().endsWith("png") || image.getFileName().startsWith(
            SMALL_PREFIX)) {
            return;
        }

        String[] parts = image.getFileName().toString().split("\\.");

        int x = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        Coordinate2D regionCoordinate = new Coordinate2D(x, z);

        Path p = image.getParent();
        regionMap.computeIfAbsent(regionCoordinate, k -> new RegionImages(p, regionCoordinate)).set(mode, p);
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

    public void drawAll(Bounds bounds, double blocksPerPixel, BiConsumer<Coordinate2D, Image> drawRegion) {
        updateRenderMode();

        regions.forEach((coordinate, images) -> {
            boolean isVisible = bounds.overlaps(coordinate);

            boolean shouldResize = images.updateSize(isVisible, imageMode, blocksPerPixel);
            if (isVisible && shouldResize) {
                imageHandlerExecutor.schedule(images::allowResample, 0, TimeUnit.MILLISECONDS);
            } else if (shouldResize) {
                resizeLater.add(images);
            }

            if (isVisible) {
                RegionImage image = images.getImage(imageMode);
                if (image == null) { return; }

                drawRegion.accept(coordinate, image.getImage());
                drawRegion.accept(coordinate, image.getChunkOverlay());
            }
        });
    }

    public String stats() {
        int size = regions.size() * 2;

        Map<Integer, Integer> counts = new HashMap<>();
        regions.forEach((k, v) -> {
            int sizeNormal = v.normal.getSize();;
            int sizeCave = v.caves.getSize();;

            counts.put(sizeNormal, counts.getOrDefault(sizeNormal, 0) + 1);
            counts.put(sizeCave, counts.getOrDefault(sizeCave, 0) + 1);
        });

        String stats = counts.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .map(e -> e.getValue() + "x" + e.getKey() + "px")
            .collect(Collectors.joining(", "));

        return size + " (" + stats + ")";
    }

    public void shutdown() {
        if (imageHandlerExecutor != null) {
            imageHandlerExecutor.shutdown();
        }
    }

    public void resetRegion(Coordinate2D region) {
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.NORMAL), NORMAL_PREFIX, region)));
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.CAVES), NORMAL_PREFIX, region)));

        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.NORMAL), SMALL_PREFIX, region)));
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension, ImageMode.CAVES), SMALL_PREFIX, region)));
        regions.remove(region);
    }
}

class RegionImages {
    Coordinate2D coordinate;
    RegionImage normal;
    RegionImage caves;

    public RegionImages(RegionImage normal, RegionImage caves) {
        this.normal = normal;
        this.caves = caves;
    }

    public RegionImages(Path p, Coordinate2D coordinate2D) {
        normal = new RegionImage(p, coordinate2D);
        caves = new RegionImage(p, coordinate2D);
        coordinate = coordinate2D;
    }

    public static RegionImages of(Dimension dimension, Coordinate2D coordinate) {
        RegionImage normal = RegionImage.of(RegionImageHandler.dimensionPath(dimension, ImageMode.NORMAL), coordinate);
        RegionImage caves = RegionImage.of(RegionImageHandler.dimensionPath(dimension, ImageMode.CAVES), coordinate);

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

    public void save() throws IOException {
        normal.save();
        caves.save();
    }

    public void set(ImageMode mode, Path image) {
        switch (mode) {
            case NORMAL -> normal = RegionImage.of(image, coordinate);
            case CAVES -> caves = RegionImage.of(image, coordinate);
        };
    }

    public boolean updateSize(boolean isVisible, ImageMode mode, double blocksPerPixel) {
        boolean shouldResize = caves.setTargetSize(isVisible && mode == ImageMode.CAVES, blocksPerPixel);
        shouldResize |= normal.setTargetSize(isVisible && mode == ImageMode.NORMAL, blocksPerPixel);

        return shouldResize;
    }

    public void allowResample() {
        caves.allowResample();
        normal.allowResample();
    }
}

