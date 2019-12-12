package game.data.chunk.palette;

import game.data.WorldManager;
import game.data.chunk.Chunk;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a palette of a chunk.
 */
public class Palette {
    private static boolean maskBedrock = false;
    public int bitsPerBlock;
    int[] palette;

    protected Palette() { }

    private Palette(int bitsPerBlock, int[] palette) {
        this.bitsPerBlock = bitsPerBlock;
        this.palette = palette;
    }


    public static void setMaskBedrock(boolean maskBedrock) {
        Palette.maskBedrock = maskBedrock;
    }

    /**
     * Read the palette from the network stream.
     * @param bitsPerBlock the number of bits per block that is used, indicates the palette type
     * @param dataTypeProvider network stream reader
     */
    public static Palette readPalette(int bitsPerBlock, DataTypeProvider dataTypeProvider) {
        int size = dataTypeProvider.readVarInt();

        int[] palette = dataTypeProvider.readVarIntArray(size);

        if (maskBedrock) {
            for (int i = 0; i < palette.length; i++) {
                if (palette[i] == 0x70) {
                    palette[i] = 0x10;
                }
            }
        }

        return new Palette(bitsPerBlock, palette);
    }

    /**
     * Get the block state from the palette index.
     */
    public int stateFromId(int index) {
        if (bitsPerBlock > 8) {
            return index;
        }

        return palette[index];
    }

    public boolean isEmpty() {
        return palette.length == 0 || (palette.length == 1 && palette[0] == 0);
    }

    /**
     * Create an NBT version of this palette using the global palette.
     */
    public List<SpecificTag> toNbt() {
        List<SpecificTag> tags = new ArrayList<>();
        GlobalPalette globalPalette = WorldManager.getGlobalPalette();

        if (globalPalette == null) {
            throw new UnsupportedOperationException("Cannot create palette NBT without a global palette.");
        }

        for (int i : palette) {
            tags.add(globalPalette.getState(i).toNbt());
        }
        return tags;
    }

    public int getIndex(long[] blocks, int x, int y, int z) {
        int individualValueMask = (1 << bitsPerBlock) - 1;

        int blockNumber = (((y * Chunk.SECTION_HEIGHT) + z) * Chunk.SECTION_WIDTH) + x;
        int startLong = (blockNumber * bitsPerBlock) / 64;
        int startOffset = (blockNumber * bitsPerBlock) % 64;
        int endLong = ((blockNumber + 1) * bitsPerBlock - 1) / 64;

        int data;
        if (startLong == endLong) {
            data = (int) (blocks[startLong] >>> startOffset);
        } else {
            int endOffset = 64 - startOffset;
            data = (int) (blocks[startLong] >>> startOffset | blocks[endLong] << endOffset);
        }
        data &= individualValueMask;

        return data;
    }
}
