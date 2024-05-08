package gui.images;

import config.Config;
import game.data.chunk.Chunk;
import game.data.coordinates.Coordinate2D;
import game.data.region.Region;
import gui.ChunkImageState;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;

public class RegionImage {
    public static final String NORMAL_PREFIX = "";
    public static final String SMALL_PREFIX = "small_";

    private static final int MIN_SIZE = 16;
    private static final long MIN_WAIT_TIME = 30 * 1000;
    private static final int SIZE = Chunk.SECTION_WIDTH * Region.REGION_SIZE;;

    // since all resizing happens on the same thread, we can re-use buffered image objects to reduce
    // memory usage
    private static final Map<Integer, BufferedImage> TEMP_IMAGES = Map.of(
        16, new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
        32, new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB),
        64, new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB),
        128, new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB),
        256, new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB),
        512, new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB)
    );
    long lastUpdated;

    int currentSize = SIZE;
    int targetSize = SIZE;

    private final Path path;
    final Coordinate2D coordinates;

    WritableImage image;
    WritableImage chunkOverlay;
    byte[] buffer;

    boolean saved;

    public RegionImage(Path path, Coordinate2D coords) {
        this(new WritableImage(MIN_SIZE, MIN_SIZE), path, coords);
        this.currentSize = MIN_SIZE;
        this.targetSize = MIN_SIZE;
    }

    private RegionImage(WritableImage image, Path path, Coordinate2D coords) {
        this.coordinates = coords;
        this.currentSize = MIN_SIZE;
        this.targetSize = MIN_SIZE;
        this.path = path;

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
        try {
            WritableImage image = loadFromFile(directoryPath, coordinate, MIN_SIZE);

            return new RegionImage(image, directoryPath, coordinate);
        } catch (Exception e) {
            return new RegionImage(directoryPath, coordinate);
        }
    }

    public boolean setTargetSize(boolean isVisible, double blocksPerPixel) {
        // don't resize if we recently wrote chunks since it is likely to be written to again
        if (!saved || System.currentTimeMillis() - lastUpdated < MIN_WAIT_TIME) {
            return false;
        }

        int newTarget = isVisible ? (int) (Math.min(Math.max(SIZE / blocksPerPixel, MIN_SIZE), SIZE)) : MIN_SIZE;
        if (newTarget != targetSize) {
            targetSize = newTarget;
            return true;
        }
        return false;
    }

    private static BufferedImage resize(BufferedImage original, int targetSize) {
        BufferedImage resizedImage = TEMP_IMAGES.get(targetSize);

        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.Clear);
        graphics2D.fillRect(0, 0, targetSize, targetSize);

        graphics2D.setComposite(AlphaComposite.DstAtop);
        graphics2D.drawImage(original, 0, 0, targetSize, targetSize, null);
        graphics2D.dispose();

        return resizedImage;
    }

    private static WritableImage loadFromFile(Path path, Coordinate2D coordinate, int targetSize) throws IOException {
        File smallFile = getFile(path, SMALL_PREFIX, coordinate);
        if (targetSize == MIN_SIZE && smallFile.exists()) {
            return loadSmall(smallFile);
        } else {
            return loadFromFile(getFile(path, NORMAL_PREFIX, coordinate), targetSize);
        }
    }

    private static WritableImage loadSmall(File file) throws IOException {
        WritableImage image = new WritableImage(MIN_SIZE, MIN_SIZE);
        SwingFXUtils.toFXImage(ImageIO.read(file), image);

        return image;
    }

    private static WritableImage loadFromFile(File file, int targetSize) throws IOException {
        WritableImage im = new WritableImage(targetSize, targetSize);

        if (file != null && file.exists()) {
            BufferedImage image = ImageIO.read(file);

            if (targetSize < SIZE) {
                image = resize(image, targetSize);
            }

            SwingFXUtils.toFXImage(image, im);

        }

        return im;
    }

    public void allowResample() {
        if (targetSize > currentSize) {
            upSample();
        } else if (targetSize < currentSize) {
            downSample();
        }
    }

    private void upSample() {
        try {
            image = loadFromFile(getFile(path, NORMAL_PREFIX, coordinates), targetSize);
            currentSize = targetSize;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downSample() {
        if (targetSize == currentSize) {
            return;
        }

        // check again in case it changed in meantime due to multithreading
        if (!saved || System.currentTimeMillis() - lastUpdated < MIN_WAIT_TIME) {
            return;
        }

        BufferedImage bufferedImage = TEMP_IMAGES.get(currentSize);
        SwingFXUtils.fromFXImage(image, bufferedImage);

        bufferedImage = resize(bufferedImage, targetSize);

        image = SwingFXUtils.toFXImage(bufferedImage, null);
        currentSize = targetSize;
    }

    public Image getImage() {
        return image;
    }

    public void drawChunk(Coordinate2D local, Image chunkImage) {
        this.lastUpdated = System.currentTimeMillis();
        this.saved = false;

        if (targetSize < SIZE || currentSize < SIZE) {
            targetSize = SIZE;

            upSample();
        }

        drawChunkToImage(local, chunkImage);
    }

    private void drawChunkToImage(Coordinate2D local, Image chunkImage) {
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

    public void save() throws IOException {
        if (saved) {
            return;
        }

        File f = getFile(path, NORMAL_PREFIX, coordinates);
        BufferedImage img = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(img, "png", f);

        img = resize(img, MIN_SIZE);
        f = getFile(path, SMALL_PREFIX, coordinates);
        ImageIO.write(img, "png", f);

        saved = true;
    }

    public static File getFile(Path p, String prefix, Coordinate2D coords) {
        return Path.of(p.toString(), prefix + filename(coords)).toFile();
    }

    private static String filename(Coordinate2D coords) {
        return "r." + coords.getX() + "." + coords.getZ() + ".png";
    }

    public Image getChunkOverlay() {
        return chunkOverlay;
    }

    public int getSize() {
        return currentSize;
    }
}

