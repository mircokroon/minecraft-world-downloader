package game.data.chunk.palette;

import game.data.WorldManager;
import game.data.chunk.Chunk;
import packets.DataTypeProvider;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a palette of a chunk.
 */
public class Palette {
    private static boolean maskBedrock = false;
    private int bitsPerBlock;
    private int[] palette;

    protected Palette() { }

    private Palette(int bitsPerBlock, int[] palette) {
        this.bitsPerBlock = bitsPerBlock;
        this.palette = palette;
    }

    public Palette(ListTag nbt) {
        this.bitsPerBlock = computeBitsPerBlock(nbt.size() - 1);
        this.palette = new int[nbt.size()];

        GlobalPalette global = WorldManager.getGlobalPalette();
        for (int i = 0; i < nbt.size(); i++) {
            BlockState bs = global.getState(nbt.get(i).get("Name").stringValue());
            this.palette[i] = bs.getNumericId();
        }
    }

    private int computeBitsPerBlock(int maxIndex) {
        int bitsNeeded = 0;
        while (maxIndex > 0) {
            bitsNeeded++;
            maxIndex >>= 1;
        }
        return Math.max(4, bitsNeeded);
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
        if (palette.length == 0) {
            return 0;
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
        if (blocks.length == 0) {
            return 0;
        }

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
