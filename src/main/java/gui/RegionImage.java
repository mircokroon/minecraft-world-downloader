package gui;

import config.Config;
import game.data.chunk.Chunk;
import game.data.coordinates.Coordinate2D;
import game.data.region.Region;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javax.imageio.ImageIO;

public class RegionImage {
    WritableImage image;
    byte[] buffer;

    public RegionImage() {
        int size = Chunk.SECTION_WIDTH * Region.REGION_DIMENSION;
        this.buffer = new byte[16 * 16 * 4];
        this.image = new WritableImage(size, size);
    }

    public Image getImage() {
        return image;
    }

    public void drawChunk(Coordinate2D local, Image chunkImage) {
        int size = Chunk.SECTION_WIDTH;

        WritablePixelFormat<ByteBuffer> format = WritablePixelFormat.getByteBgraInstance();
        chunkImage.getPixelReader().getPixels(0, 0, size, size, format, buffer, 0, size * 4);
        image.getPixelWriter().setPixels(local.getX() * size, local.getZ() * size, size, size, format, buffer, 0, size * 4);
    }

    public void export(Coordinate2D coords) throws IOException {
        String name = "r." + coords.getX() + "." + coords.getZ() + ".png";;
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", Path.of(Config.getWorldOutputDir(), name).toFile());
    }
}
