package game.data.chunk.version.encoder;

import game.data.chunk.Chunk;

public class BlockLocationEncoder {
    int individualValueMask;
    int startLong;
    int startOffset;
    int endLong;

    public BlockLocationEncoder() {
    }

    public int fetch(long[] blocks) {
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

    public void write(long[] blocks, int newIndex) {
        long data = newIndex & individualValueMask;

        // first set all relevant bits to 0, then use or to put the new bits in place
        blocks[startLong] &= ~((long) individualValueMask << startOffset);
        blocks[startLong] |= (data << startOffset);

        if (startLong != endLong) {
            blocks[endLong] &= ~(individualValueMask >> (64 - startOffset));
            blocks[endLong] |= (data >> (64 - startOffset));
        }
    }

    public BlockLocationEncoder setTo(int x, int y, int z, int bitsPerBlock) {
        this.individualValueMask = (1 << bitsPerBlock) - 1;

        int blockNumber = (((y * Chunk.SECTION_HEIGHT) + z) * Chunk.SECTION_WIDTH) + x;
        this.startLong = (blockNumber * bitsPerBlock) / 64;
        this.startOffset = (blockNumber * bitsPerBlock) % 64;
        this.endLong = ((blockNumber + 1) * bitsPerBlock - 1) / 64;

        return this;
    }
}
