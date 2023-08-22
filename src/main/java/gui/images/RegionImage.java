package gui.images;

import config.Config;
import game.data.chunk.Chunk;
import game.data.coordinates.Coordinate2D;
import game.data.region.Region;
import gui.ChunkImageState;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;

public class RegionImage {
    private static final long MIN_WAIT_TIME = 30 * 1000;
    private static final int SIZE = Chunk.SECTION_WIDTH * Region.REGION_SIZE;;
    private File file;
    long lastUpdated;

    ConcurrentLinkedQueue<Runnable> afterUpscale;

    int currentSize = SIZE;
    int targetSize = SIZE;

    WritableImage image;
    WritableImage chunkOverlay;
    byte[] buffer;

    boolean saved;

    public RegionImage() {
        this(new WritableImage(SIZE, SIZE));
    }

    private RegionImage(WritableImage image, File file) {
        this(image);

        this.file = file;
    }

    private RegionImage(WritableImage image) {
        this.image = image;
        this.buffer = new byte[16 * 16 * 4];
        this.saved = true;
        this.afterUpscale = new ConcurrentLinkedQueue<>();

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

    public void setTargetSize(boolean isVisible, double blocksPerPixel) {
        if (!saved || System.currentTimeMillis() - lastUpdated < MIN_WAIT_TIME) {
            return;
        }

        if (!isVisible) {
            targetSize = 16;
            return;
        }
        targetSize = (int) (SIZE / Math.max(Math.min(blocksPerPixel, 32), 1));
    }

    private static WritableImage reloadFromFile(File image) throws IOException {
        WritableImage im = new WritableImage(SIZE, SIZE);
        if (image != null && image.exists()) {
            SwingFXUtils.toFXImage(ImageIO.read(image), im);
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
            image = reloadFromFile(file);
            currentSize = SIZE;

            while (!afterUpscale.isEmpty()) {
                afterUpscale.remove().run();
            }
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

        Canvas c = new Canvas(targetSize, targetSize);
        c.getGraphicsContext2D().setImageSmoothing(true);
        c.getGraphicsContext2D().drawImage(image,0, 0, targetSize, targetSize);

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        WritableImage dst = new WritableImage(targetSize, targetSize);

        // TODO - gotta do on the UI thread, need to write custom implementation for downsampling :(
        Platform.runLater(() -> {
            // check AGAIN just to be safe ... this won't be needed after this is rewritten
            if (!saved || System.currentTimeMillis() - lastUpdated < MIN_WAIT_TIME) {
                return;
            }
            image = c.snapshot(snapshotParameters, dst);
            currentSize = targetSize;
        });
    }

    public static RegionImage of(File file) {
        try {
            WritableImage im = reloadFromFile(file);

            return new RegionImage(im, file);
        } catch (IOException e) {
            return new RegionImage();
        }
    }

    public Image getImage() {
        return image;
    }

    public void drawChunk(Coordinate2D local, Image chunkImage) {
        lastUpdated = System.currentTimeMillis();

        if (targetSize < SIZE) {
            targetSize = SIZE;
        }

        if (currentSize < SIZE) {
            targetSize = SIZE;

            // will draw image on the imager handler thread if it has to be resized first
            afterUpscale.add(() -> {
                drawChunkToImage(local, chunkImage);
            });
            return;
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

