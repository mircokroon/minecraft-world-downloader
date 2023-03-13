package gui;

import config.Config;
import game.data.chunk.Chunk;
import game.data.coordinates.Coordinate2D;
import game.data.region.Region;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;

public class RegionImage {
    private static final int SIZE = Chunk.SECTION_WIDTH * Region.REGION_SIZE;;
    WritableImage image;
    WritableImage chunkOverlay;
    byte[] buffer;

    boolean saved;

    public RegionImage() {
        this(new WritableImage(SIZE, SIZE));
    }

    private RegionImage(WritableImage image) {
        this.image = image;
        this.buffer = new byte[16 * 16 * 4];
        this.saved = true;

        chunkOverlay = new WritableImage(Region.REGION_SIZE, Region.REGION_SIZE);

        // if mark old chunks is enabled, the overlay is initialised to the same as the GUI
        // background color (with some opacity). Newly loaded chunks will make this transparent
        // when loaded in.
        if (Config.markOldChunks()) {
            fillOverlay(ChunkImageState.OUTDATED.getColor());
        }
    }

    /**
     * Fills overlay with the given colour.
     */
    private void fillOverlay(Color c) {
        for (int i = 0; i < Region.REGION_SIZE; i++) {
            for (int j = 0; j < Region.REGION_SIZE; j++) {
                chunkOverlay.getPixelWriter().setColor(i, j, c);
            }
        }
    }

    public static RegionImage of(Path directoryPath, Coordinate2D coordinate) {
        File file = Paths.get(directoryPath.toString(), filename(coordinate)).toFile();

        if (!file.exists()) {
            return new RegionImage();
        }

        return of(file);
    }

    public static RegionImage of(File image) {
        try {
            WritableImage im = new WritableImage(SIZE, SIZE);
            SwingFXUtils.toFXImage(ImageIO.read(image), im);

            return new RegionImage(im);
        } catch (IOException e) {
            return new RegionImage();
        }
    }

    public Image getImage() {
        return image;
    }

    public void drawChunk(Coordinate2D local, Image chunkImage) {
        int size = Chunk.SECTION_WIDTH;

        WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
        chunkImage.getPixelReader().getPixels(0, 0, size, size, format, buffer, 0, size * 4);
        image.getPixelWriter().setPixels(local.getX() * size, local.getZ() * size, size, size, format, buffer, 0, size * 4);

        saved = false;
    }

    public void colourChunk(Coordinate2D local, Color color) {
        if (chunkOverlay != null) {
            chunkOverlay.getPixelWriter().setColor(local.getX(), local.getZ(), color);
        }
    }

    public void save(Path p, Coordinate2D coords) throws IOException {
        if (saved) {
            return;
        }
        saved = true;

        File f = getFile(p, coords);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", f);
    }

    public static File getFile(Path p, Coordinate2D coords) {
        return Path.of(p.toString(), filename(coords)).toFile();
    }

    private static String filename(Coordinate2D coords) {
        return "r." + coords.getX() + "." + coords.getZ() + ".png";
    }

    public Image getChunkOverlay() {
        return chunkOverlay;
    }
}

