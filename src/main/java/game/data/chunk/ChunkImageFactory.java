package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.SimpleColor;
import game.data.chunk.palette.blending.IBlendEquation;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles creating images from a Chunk.
 */
public class ChunkImageFactory {
    public static boolean fail = false;
    private final Chunk c;
    private final List<CoordinateDim2D> registeredCallbacks = new ArrayList<>(2);
    private Consumer<Image> onImageDone;

    public ChunkImageFactory(Chunk c) {
        this.c = c;

        c.setOnUnload(this::unload);

        computeHeightMap();
        WorldManager.getInstance().chunkLoadedCallback(c.location);
    }

    /**
     * Set handler for when the image has been created.
     */
    public void onComplete(Consumer<Image> onComplete) {
        this.onImageDone = onComplete;
    }


    private void registerChunkLoadCallback(CoordinateDim2D coordinates) {
        registeredCallbacks.add(coordinates);
        WorldManager.getInstance().registerChunkLoadCallback(coordinates, this::createImage);
    }

    public void unload() {
        for (CoordinateDim2D coordinates : registeredCallbacks) {
            WorldManager.getInstance().deregisterChunkLoadCallback(coordinates, this::createImage);
        }
    }

    /**
     * Compares the blocks south and north, use the gradient to get a multiplier for the colour.
     * @return a colour multiplier to adjust the color value by. If they elevations are the same it will be 1.0, if the
     * northern block is above the current its 0.8, otherwise its 1.2.
     */
    private double getColorShader(int x, int z) {
        int yNorth = getOtherHeight(x, z, 0, -1);
        if (yNorth < 0) { return 1; }

        int ySouth = getOtherHeight(x, z, 15, 1);
        if (ySouth < 0) { return 1; }

        if (ySouth < yNorth) {
            return 0.6 + (0.4 / (1 + yNorth - ySouth));
        } else if (ySouth > yNorth) {
            return 1.9 - (0.8 / Math.sqrt(ySouth - yNorth));
        }
        return 1;
    }

    /**
     * Get the height of a neighbouring block. If the block is not on this chunk, either load it or register a callback
     * for when it becomes available.
     */
    private int getOtherHeight(int x, int z, int zLimit, int offsetZ) {
        if (z != zLimit) {
            return c.heightAt(x, z + offsetZ);
        }

        CoordinateDim2D coordinate = c.location.addWithDimension(0, offsetZ);
        Chunk other = WorldManager.getInstance().getChunk(coordinate);

        if (other == null || !other.hasHeightMaps()) {
            registerChunkLoadCallback(coordinate);
            return -1;
        } else {
            return other.heightAt(x, 15 - zLimit);
        }
    }


    /**
     * Generate and return the overview image for this chunk.
     */
    public void createImage() {
        System.out.println("creating " + c.location);
        if (c.location.getZ() == -6 && c.location.getX() == -6) {
            new RuntimeException().printStackTrace();
        }

        WritableImage i = new WritableImage(16, 16);
        PixelWriter writer = i.getPixelWriter();

        try {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = c.heightAt(x, z);
                    BlockState blockState = c.getBlockStateAt(x, y, z);

                    SimpleColor color;
                    if (blockState == null) {
                        color = SimpleColor.TRANSPARENT;
                    } else {
                        color = shadeTransparent(blockState, x, y, z);
                    }

                    color = color.shaderMultiply(getColorShader(x, z));

                    writer.setColor(x, z, color.toJavaFxColor());

                    // mark new chunks in a red-ish outline
                    if (c.isNewChunk() && ((x == 0 || x == 15) || (z == 0 || z == 15))) {
                        writer.setColor(x, z, color.highlight().toJavaFxColor());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Unable to draw picture for chunk at " + c.location);
            ex.printStackTrace();
        }

        if (this.onImageDone != null) {
            this.onImageDone.accept(i);
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

    protected void computeHeightMap() {
        int[] heightMap = new int[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH];

        for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
            for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {
                heightMap[z << 4 | x] = computeHeight(x, z);
            }
        }

        c.setHeightMap(heightMap);
    }

    /**
     * Computes the height at a given location. When we are in the nether, we want to try and make it clear where there
     * is an opening, and where there is not. For this we skip the first two chunks sections (these will be mostly solid
     * anyway, but may contain misleading caves). We then only count blocks after we've found some air space.
     */
    private int computeHeight(int x, int z) {
        // if we're in the Nether, we want to find an air block before we start counting blocks.
        boolean isNether = c.location.getDimension().equals(Dimension.NETHER);
        int topSection = isNether ? 5 : 15;

        MutableBoolean foundAir = new MutableBoolean(!isNether);

        for (int chunkSection = topSection; chunkSection >= 0; chunkSection--) {
            ChunkSection cs = c.getChunkSections()[chunkSection];
            if (cs == null) {
                foundAir.setTrue();
                continue;
            }

            int height = cs.computeHeight(x, z, foundAir);

            if (height < 0) { continue; }

            // if we're in the nether we can't find
            if (isNether && chunkSection == topSection && height == 15) {
                return 127;
            }
            return (chunkSection * Chunk.SECTION_HEIGHT) + height;
        }
        return isNether ? 127 : 0;
    }
}
