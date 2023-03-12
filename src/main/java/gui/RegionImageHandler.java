package gui;

import static util.ExceptionHandling.attempt;
import static util.ExceptionHandling.attemptQuiet;

import config.Config;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
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
    private static final Color SAVED = Color.TRANSPARENT;
    private static final Color UNSAVED = Color.color(1, 0, 0, .35);

    private static final String CACHE_PATH = "image-cache";
    private Map<Coordinate2D, RegionImage> regions;
    private Dimension activeDimension;
    private boolean isSaving = false;

    private final ScheduledExecutorService saveService;

    public RegionImageHandler() {
        this.regions = new ConcurrentHashMap<>();

        // TODO: remove or compress overview images far away to reduce memory usage
        saveService = Executors.newSingleThreadScheduledExecutor(
            (r) -> new Thread(r, "Region Image Handler")
        );
        saveService.scheduleWithFixedDelay(this::save, 20, 20, TimeUnit.SECONDS);
    }

    public void clear() {
        unload();
        attemptQuiet(() -> FileUtils.deleteDirectory(Paths.get(Config.getWorldOutputDir(), CACHE_PATH).toFile()));
    }

    public void drawChunk(CoordinateDim2D coordinate, Image chunkImage, Boolean isSaved) {
        if (!coordinate.getDimension().equals(activeDimension)) {
            return;
        }

        Coordinate2D region = coordinate.chunkToRegion();

        RegionImage image = regions.computeIfAbsent(region, (coordinate2D -> loadRegion(coordinate)));

        Coordinate2D local = coordinate.toRegionLocal();
        image.drawChunk(local, chunkImage);

        setChunkSavedStatus(image, local, isSaved);
    }

    private void setChunkSavedStatus(Coordinate2D coordinate, boolean isSaved) {
        colourChunk(coordinate, Config.markUnsavedChunks() && !isSaved ? UNSAVED : SAVED);
    }

    private void setChunkSavedStatus(RegionImage region, Coordinate2D local, boolean isSaved) {
        colourChunk(region, local, Config.markUnsavedChunks() && !isSaved ? UNSAVED : SAVED);
    }

    public void colourChunk(Coordinate2D coords, Color color) {
        Coordinate2D region = coords.chunkToRegion();

        RegionImage image = regions.get(region);
        if (image == null) {
            return;
        }

        colourChunk(image, coords.toRegionLocal(), color);
    }

    private void colourChunk(RegionImage image, Coordinate2D local, Color color) {
        image.colourChunk(local, color);
    }


    public void markChunkSaved(CoordinateDim2D coordinate) {
        setChunkSavedStatus(coordinate, true);
    }

    private RegionImage loadRegion(Coordinate2D coordinate) {
        return RegionImage.of(dimensionPath(this.activeDimension), coordinate);
    }

    private void save(Map<Coordinate2D, RegionImage> regions, Dimension dim) {
        // if shutdown is called, wait for saving to complete
        if (isSaving) {
            if (saveService != null) {
                attempt(() -> saveService.awaitTermination(10, TimeUnit.SECONDS));
            }
            return;
        }
        isSaving = true;

        attempt(() -> Files.createDirectories(dimensionPath(dim)));
        regions.forEach((coordinate, image) -> {
            attempt(() -> image.save(dimensionPath(dim), coordinate));
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
        Map<Coordinate2D, RegionImage> regionMap = regions;

        new Thread(() -> attemptQuiet(() -> {
            Files.walk(dimensionPath(this.activeDimension), 1).limit(3200)
                .forEach(image -> attempt(() -> {
                if (!image.toString().toLowerCase().endsWith("png")) {
                    return;
                }

                String[] parts = image.getFileName().toString().split("\\.");

                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                Coordinate2D regionCoordinate = new Coordinate2D(x, z);

                regionMap.put(regionCoordinate, RegionImage.of(image.toFile()));
            }));
        })).start();
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

    private static Path dimensionPath(Dimension dim) {
        return Paths.get(Config.getWorldOutputDir(), CACHE_PATH, dim.getPath());
    }

    public void drawAll(Bounds bounds, BiConsumer<Coordinate2D, Image> drawRegion) {
        regions.forEach((coordinate, image) -> {
            if (bounds.overlaps(coordinate)) {
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
        attemptQuiet(() -> FileUtils.delete(RegionImage.getFile(dimensionPath(this.activeDimension), region)));
        regions.remove(region);
    }
}
