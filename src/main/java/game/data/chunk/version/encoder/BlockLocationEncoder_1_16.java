package game.data.chunk.version.encoder;

import game.data.chunk.Chunk;

/**
 * 1.16 needs a a slightly different getPaletteIndex method. Instead of a blockstate now overlapping multiple longs,
 * it will push the next index to the next long (so some bits at the end of each long may go unused). Luckily, this
 * actually makes the method a little bit simpler.
 */
public class BlockLocationEncoder_1_16 extends BlockLocationEncoder {
    int longIndex;

    public BlockLocationEncoder_1_16() {
        this.startLong = -1;
        this.endLong = -1;
    }

    public int fetch(long[] blocks) {
        int data = (int) (blocks[longIndex] >>> startOffset);
        data &= individualValueMask;

        return data;
    }

    public void write(long[] blocks, int newIndex) {
        long data = newIndex & individualValueMask;

        // first set all relevant bits to 0, then use or to put the new bits in place
        blocks[longIndex] &= ~((long) individualValueMask << startOffset);
        blocks[longIndex] |= (data << startOffset);
    }

    public BlockLocationEncoder setTo(int x, int y, int z, int bitsPerBlock) {
        this.individualValueMask = (1 << bitsPerBlock) - 1;

        int blockNumber = (((y * Chunk.SECTION_HEIGHT) + z) * Chunk.SECTION_WIDTH) + x;

        // bitsPerBlock can be 0 if we're trying to call the BlockLocationEncoder
        // on a SingleValuePalette. Doing so would cause division by 0 errors!
        if (bitsPerBlock == 0) {
            longIndex = 0;
            startOffset = 0;
        } else {
            int blocksPerLong = 64 / bitsPerBlock;
            this.longIndex = blockNumber / blocksPerLong;
            int indexInLong = blockNumber % blocksPerLong;
            this.startOffset = indexInLong * bitsPerBlock;
        }

        if (longIndex < 0) {
            System.out.println("INVALID LONG INDEX: ");
            System.out.println("\t" + x +", " + y + ", " + z + " :: " + bitsPerBlock);
        }

        return this;
    }
}
