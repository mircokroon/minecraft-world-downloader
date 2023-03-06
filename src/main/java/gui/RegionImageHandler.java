package gui;

import static util.ExceptionHandling.attempt;
import static util.ExceptionHandling.attemptQuiet;

import config.Config;
import game.data.coordinates.Coordinate2D;
import game.data.dimension.Dimension;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javafx.scene.image.Image;

public class RegionImageHandler {
    private Map<Coordinate2D, RegionImage> regions;
    private Dimension activeDimension;
    private Set<Coordinate2D> activeRegions;

    public RegionImageHandler() {
        this.regions = new HashMap<>();
        this.activeRegions = new HashSet<>();

        ScheduledExecutorService
            executor = Executors.newSingleThreadScheduledExecutor((r) -> new Thread(r, "Region Image Handler"));
        executor.scheduleWithFixedDelay(this::optimise, 20, 20, TimeUnit.SECONDS);
    }

    private void optimise() {
        export();
        // TODO
    }

    public void clear() {
        // TODO
    }

    public void drawChunk(Coordinate2D coordinate, Image chunkImage) {
        Coordinate2D region = coordinate.chunkToRegion();

        RegionImage image = regions.computeIfAbsent(region, (coordinate2D -> loadRegion(coordinate)));

        Coordinate2D local = coordinate.toRegionLocal();
        image.drawChunk(local, chunkImage);
    }

    private RegionImage loadRegion(Coordinate2D coordinate) {
        activeRegions.add(coordinate);

        return RegionImage.of(dimensionPath(this.activeDimension), coordinate);
    }

    public void export(Map<Coordinate2D, RegionImage> regions, Dimension dim) {
        attempt(() -> Files.createDirectories(dimensionPath(dim)));
        regions.forEach((coordinate, image) -> {
            attempt(() -> image.export(dimensionPath(dim), coordinate));
        });
    }

    public void export() {
        export(this.regions, this.activeDimension);
    }

    private void unload() {
        export(this.regions, this.activeDimension);
        this.regions = new HashMap<>();
    }

    private void load() throws IOException {
        Files.walk(dimensionPath(this.activeDimension), 1).forEach(image -> attempt(() ->{
            if (!image.toString().toLowerCase().endsWith("png")) {
                return;
            }

            String[] parts = image.getFileName().toString().split("\\.");

            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            Coordinate2D regionCoordinate = new Coordinate2D(x, z);

            regions.put(regionCoordinate, RegionImage.of(image.toFile()));
        }));
    }

    public void setDimension(Dimension dimension) {
        System.out.println("Set dimension to " + dimension);

        if (this.activeDimension != null) {
            unload();
        }

        this.activeDimension = dimension;
        attemptQuiet(this::load);
    }

    private static Path dimensionPath(Dimension dim) {
        return Paths.get(Config.getWorldOutputDir(), "downloader-overview-cache", dim.getPath());
    }

    public void drawAll(Bounds bounds, BiConsumer<Coordinate2D, Image> drawRegion) {
        regions.forEach((coordinate, image) -> {
            drawRegion.accept(coordinate, image.getImage());
        });
    }

    public int size() {
        return regions.size();
    }
}
