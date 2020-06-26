package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.Tag;

public class ChunkSection_1_16 extends ChunkSection_1_13 {
    public ChunkSection_1_16(byte y, Palette palette) {
        super(y, palette);
    }

    public ChunkSection_1_16(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }

    /**
     * 1.16 needs a a slightly different getPaletteIndex method. Instead of a blockstate now overlapping multiple longs,
     * it will push the next index to the next long (so some bits at the end of each long may go unused). Luckily, this
     * actually makes the method a little bit simpler.
     */
    @Override
    public int getPaletteIndex(int x, int y, int z) {
        if (blocks.length == 0) {
            return 0;
        }

        int bitsPerBlock = palette.getBitsPerBlock();
        int individualValueMask = (1 << bitsPerBlock) - 1;

        int blockNumber = (((y * Chunk.SECTION_HEIGHT) + z) * Chunk.SECTION_WIDTH) + x;

        int blocksPerLong = 64 / bitsPerBlock;
        int longIndex = blockNumber / blocksPerLong;
        int indexInLong = blockNumber % blocksPerLong;
        int startOffset = indexInLong * bitsPerBlock;

        int data = (int) (blocks[longIndex] >>> startOffset);
        data &= individualValueMask;

        return data;
    }

}
