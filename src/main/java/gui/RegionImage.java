package gui;

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
import javax.imageio.ImageIO;

public class RegionImage {
    private static final int SIZE = Chunk.SECTION_WIDTH * Region.REGION_DIMENSION;;
    WritableImage image;
    byte[] buffer;

    boolean saved;

    public RegionImage() {
        this(new WritableImage(SIZE, SIZE));
    }

    private RegionImage(WritableImage image) {
        this.image = image;
        this.buffer = new byte[16 * 16 * 4];
        this.saved = false;
    }

    public static RegionImage of(Path directoryPath, Coordinate2D coordinate) {
        File file = Paths.get(directoryPath.toString(), fileName(coordinate)).toFile();

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

    public void export(Path p, Coordinate2D coords) throws IOException {
        if (saved) {
            return;
        }
        saved = true;

        File f = Path.of(p.toString(), fileName(coords)).toFile();
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", f);

    }

    private static String fileName(Coordinate2D coords) {
        return "r." + coords.getX() + "." + coords.getZ() + ".png";
    }
}