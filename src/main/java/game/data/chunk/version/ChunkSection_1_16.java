package game.data.chunk.version;

import config.Version;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import game.data.chunk.version.encoder.BlockLocationEncoder_1_16;
import se.llbit.nbt.Tag;

public class ChunkSection_1_16 extends ChunkSection_1_15 {
    public static final Version VERSION = Version.V1_16;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    private final BlockLocationEncoder blockLocationEncoder = new BlockLocationEncoder_1_16();

    @Override
    protected BlockLocationEncoder getLocationEncoder() {
        return blockLocationEncoder;
    }

    public ChunkSection_1_16(byte y, Palette palette) {
        super(y, palette);
    }

    public ChunkSection_1_16(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }

    /**
     * When the bits per block increases, we must rewrite the blocks array.
     */
    @Override
    public synchronized void resizeBlocks(int newBitsPerBlock) {
        int blocksPerLong = 64 / newBitsPerBlock;
        int newSize = (int) Math.ceil(4096.0 / blocksPerLong);
        long[] newBlocks = new long[newSize];

        if (blocks == null) {
            this.blocks = newBlocks;
            return;
        }

        copyBlocks(newBlocks, newBitsPerBlock);
    }
}

