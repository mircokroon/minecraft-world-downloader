package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.SimpleColor;
import game.data.chunk.palette.blending.IBlendEquation;
import game.data.coordinates.CoordinateDim2D;
import gui.images.ImageMode;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

/**
 * Handles creating images from a Chunk.
 */
public class ChunkImageFactory {
    private final List<CoordinateDim2D> registeredCallbacks = new ArrayList<>(2);
    private final Runnable requestImage = this::requestImage;
    ;
    private BiConsumer<Map<ImageMode, Image>, Boolean> onImageDone;
    private Runnable onSaved;

    private final Chunk c;
    private Chunk south;
    private Chunk north;

    private boolean drawnBefore = false;

    public ChunkImageFactory(Chunk c) {
        this.c = c;

        c.setOnUnload(this::unload);
    }

    /**
     * Since image factories will call upon image factories of neighbouring chunks, we need to make
     * sure it can be assigned first so that we don't end up making duplicates and causing memory
     * leaks. Initialise should be called immediately after assignment.
     */
    public void initialise() {
        WorldManager.getInstance().chunkLoadedCallback(c);
    }

    /**
     * Set handler for when the image has been created.
     */
    public void onComplete(BiConsumer<Map<ImageMode, Image>, Boolean> onComplete) {
        this.onImageDone = onComplete;
    }

    public void onSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    public void markSaved() {
        if (this.onSaved != null) {
            this.onSaved.run();
        }
    }

    private void registerChunkLoadCallback(CoordinateDim2D coordinates) {
        registeredCallbacks.add(coordinates);
        WorldManager.getInstance().registerChunkLoadCallback(coordinates, requestImage);
    }

    public void unload() {
        for (CoordinateDim2D coords : registeredCallbacks) {
            WorldManager.getInstance().deregisterChunkLoadCallback(coords, requestImage);
        }
    }

    /**
     * Compares the blocks south and north, use the gradient to get a multiplier for the colour.
     *
     * @return a colour multiplier to adjust the color value by. If they elevations are the same it will be 1.0, if the
     * northern block is above the current its 0.8, otherwise its 1.2.
     */
    private double getColorShader(int x, int y, int z, boolean ignoreBedrock) {
        int yNorth = getOtherHeight(x, z, 0, -1, north, ignoreBedrock);
        if (yNorth < 0) { yNorth = y; }

        int ySouth = getOtherHeight(x, z, 15, 1, south, ignoreBedrock);
        if (ySouth < 0) { ySouth = y; }

        if (ySouth < yNorth) {
            return 0.6 + (0.4 / (1 + yNorth - ySouth));
        } else if (ySouth > yNorth) {
            return 1.6 - (0.6 / Math.sqrt(ySouth - yNorth));
        }
        return 1;
    }

    /**
     * Get the height of a neighbouring block. If the block is not on this chunk, either load it or register a callback
     * for when it becomes available.
     */
    private int getOtherHeight(int x, int z, int zLimit, int offsetZ, Chunk other, boolean ignoredBedrock) {
        if (z != zLimit) {
            return heightAt(x, z + offsetZ, ignoredBedrock);
        }

        if (other == null || other.getChunkHeightHandler() == null) {
            return -1;
        } else {
            return other.getChunkHeightHandler().heightAt(x, 15 - zLimit, ignoredBedrock);
        }
    }


    public void requestImage() {
        // this method is only called either the first time by the UI, or subsequently by callbacks
        // when adjacent chunks load in. This means that if we only had a single callback registered
        // we don't need to worry about de-registering it anymore.
        if (drawnBefore && registeredCallbacks.size() == 1) {
            registeredCallbacks.clear();
        }

        generateImages();
    }

    private SimpleColor getColorCave(int x, int z) {
        List<Cave> caves = c.getChunkHeightHandler().getCaves(x, z);

        if (caves.isEmpty()) {
            return new SimpleColor(0);
        }

        SimpleColor c = caves.get(0).getColor();

        for (int i = 1; i < caves.size(); i++) {
            SimpleColor next = caves.get(i).getColor();

            c = c.blendWith(next, 1.0 / (i + 1));
        }

        return c;
    }

    private SimpleColor getColorSurface(int x, int z, boolean useIgnoredBedrock) {
        int y = heightAt(x, z, useIgnoredBedrock);
        BlockState blockState = c.getBlockStateAt(x, y, z);

        if (blockState == null) {
            return new SimpleColor(0);
        }

        SimpleColor color = shadeTransparent(blockState, x, y, z);

        color = color.shaderMultiply(getColorShader(x, y, z, useIgnoredBedrock));

        // mark new chunks in a red-ish outline
        if (c.isNewChunk() && ((x == 0 || x == 15) || (z == 0 || z == 15))) {
            color = color.highlight();
        }

        return color;
    }


    private Image createImage(boolean isSurface) {
        WritableImage i = new WritableImage(Chunk.SECTION_WIDTH, Chunk.SECTION_WIDTH);
        int[] output = new int[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH];
        WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();

        // setup north/south chunks
        setupAdjacentChunks();
        drawnBefore = true;

        boolean isNether = c.getDimension().isNether();
        try {
            for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
                for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {

                    SimpleColor color = isSurface ? getColorSurface(x, z, false) : isNether ? getColorSurface(x, z, true) : getColorCave(x, z);

                    output[x + Chunk.SECTION_WIDTH * z] = color.toARGB();
                }
            }
            i.getPixelWriter().setPixels(
                0, 0,
                Chunk.SECTION_WIDTH, Chunk.SECTION_WIDTH,
                format, output, 0, Chunk.SECTION_WIDTH
            );
        } catch (Exception ex) {
            System.out.println("Unable to draw picture for chunk at " + c.location);
            ex.printStackTrace();
            clearAdjacentChunks();
        }
        return i;
    }




    /**
     * Generate and return the overview image for this chunk.
     */
    void generateImages() {
        if (this.onImageDone != null) {
            Map<ImageMode, Image> map = Map.of(
                ImageMode.NORMAL, createImage(true),
                ImageMode.CAVES, createImage(false)
            );
            this.onImageDone.accept(map, c.isSaved());
        }
        clearAdjacentChunks();
    }

    /**
     * Clear references to north/south chunks after drawing. If we don't do this we may keep the
     * chunks from getting GCd causing memory leaks (especially when moving long distances in north/
     * south direction)
     */
    private void clearAdjacentChunks() {
        this.north = null;
        this.south = null;
    }

    private void setupAdjacentChunks() {
        CoordinateDim2D coordinateSouth = c.location.addWithDimension(0, 1);
        this.south = WorldManager.getInstance().getChunk(coordinateSouth);

        CoordinateDim2D coordinateNorth = c.location.addWithDimension(0, -1);
        this.north = WorldManager.getInstance().getChunk(coordinateNorth);

        if (!drawnBefore) {
            if (this.south == null) {
                registerChunkLoadCallback(coordinateSouth);
            }
            if (this.north == null) {
                registerChunkLoadCallback(coordinateNorth);
            }
        }
    }

    private SimpleColor shadeTransparent(BlockState blockState, int x, int y, int z) {
        SimpleColor color = blockState.getColor();
        BlockState next;
        for (int level = y - 1; blockState.isTransparent() && level >= 0; level--) {
            next = c.getBlockStateAt(x, level, z);

            if (next == blockState) {
                continue;
            } else if (next == null) {
                break;
            }

            IBlendEquation equation = blockState.getTransparencyEquation();
            double ratio = equation.getRatio(y - level);
            color = color.blendWith(next.getColor(), ratio);

            // stop once the contribution to the colour is less than 10%
            if (ratio > 0.90) {
                break;
            }
            blockState = next;
        }
        return color;
    }

    private int heightAt(int x, int z, boolean ignoreBedrock) {
        return c.getChunkHeightHandler().heightAt(x, z, ignoreBedrock);
    }

    @Override
    public String toString() {
        return "ChunkImageFactory{" +
            "c=" + c.location +
            '}';
    }
}


