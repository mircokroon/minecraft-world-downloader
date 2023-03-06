package gui;

import game.data.coordinates.Coordinate2D;
import game.data.dimension.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javafx.scene.image.Image;

public class RegionImageHandler {
    private Map<Coordinate2D, RegionImage> images;

    public RegionImageHandler() {
        this.images = new HashMap<>();
    }

    public void clear() {
        // TODO
    }

    public void drawChunk(Coordinate2D coordinate, Image chunkImage) {
        Coordinate2D region = coordinate.chunkToRegion();

        RegionImage image = images.computeIfAbsent(region, (coordinate2D -> new RegionImage()));

        Coordinate2D local = coordinate.toRegionLocal();
        image.drawChunk(local, chunkImage);
    }

    public void export() {
        images.forEach((coordinate, image) -> {
            try {
                image.export(coordinate);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public void setDimension(Dimension dimension) {
        // TODO
    }

    public void drawAll(Bounds bounds, BiConsumer<Coordinate2D, Image> drawRegion) {
        images.forEach((coordinate, image) -> {
            drawRegion.accept(coordinate, image.getImage());
        });
    }
}
