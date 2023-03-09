package game.data.chunk.version;

import config.Version;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import game.data.chunk.version.encoder.BlockLocationEncoder_1_16;
import se.llbit.nbt.Tag;

public class ChunkSection_1_16 extends ChunkSection_1_15 {
    private final BlockLocationEncoder blockLocationEncoder = new BlockLocationEncoder_1_16();

    @Override
    protected BlockLocationEncoder getLocationEncoder() {
        return blockLocationEncoder;
    }

    public ChunkSection_1_16(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }

    public ChunkSection_1_16(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, nbt, chunk);
    }

    /**
     * When the bits per block increases, we must rewrite the blocks array.
     */
    @Override
    public synchronized void resizeBlocksIfRequired(int newBitsPerBlock) {
        int newSize = longsRequired(newBitsPerBlock);

        // if blocks is empty or isn't the correct size, no need to copy
        if (blocks == null || blocks.length != longsRequired(palette.getBitsPerBlock())) {
            this.blocks = new long[newSize];
            return;
        }

        // if the length didn't change we don't have to do anything
        if (blocks.length == newSize) {
            return;
        }

        copyBlocks(new long[newSize], newBitsPerBlock);
    }

    public static int longsRequired(int bitsPerBlock) {
        return longsRequired(bitsPerBlock, 4096);
    }

    public static int longsRequiredBiomes(int bitsPerBlock) {
        return longsRequired(bitsPerBlock, 64);
    }

    private static int longsRequired(int bitsPerBlock, double totalItems) {
        if (bitsPerBlock == 0) {
            return 0;
        }

        int blocksPerLong = 64 / bitsPerBlock;
        return (int) Math.ceil(totalItems / blocksPerLong);
    }
}

