package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;

/**
 * Chunk sections in 1.12 require parsing of the full block data as the level format does not include a palette
 * as the new versions do. As such this class is more lengthy than the new versions.
 */
public class ChunkSection_1_12 extends ChunkSection {
    protected int[][][] blockStates;
    private int bitsPerBlock;
    public ChunkSection_1_12(byte y, Palette palette, int bitsPerBlock) {
        super(y, palette);
        this.bitsPerBlock = bitsPerBlock;
        this.blockStates = new int[16][16][16];
    }

    @Override
    public void setBlocks(long[] blocks) {
        super.setBlocks(blocks);
        for (int y = 0; y < Chunk.SECTION_HEIGHT; y++) {
            for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {
                for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
                    int data = getPaletteIndex(bitsPerBlock, x, y, z);

                    this.blockStates[x][y][z] = palette.stateFromId(data);
                }
            }
        }
    }

    @Override
    protected void addNbtTags(CompoundTag map) {
        map.add("Blocks", new ByteArrayTag(getBlockIds()));
        map.add("Data", new ByteArrayTag(getBlockStates()));
    }

    private byte[] getBlockIds() {
        byte[] blockData = new byte[4096];

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    blockData[getBlockIndex(x, y, z)] = (byte) (blockStates[x][y][z] >>> 4);
                }
            }
        }
        return blockData;
    }

    private byte[] getBlockStates() {
        byte[] blockData = new byte[2048];

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    insertAtHalf(blockData, x, y, z, (blockStates[x][y][z]) & 0x0F);
                }
            }
        }
        return blockData;
    }

    /**
     * Handle inserting of the four-bit values into the given array at the given coordinates. In this case, a value
     * takes up 4 bits so inserting them is a bit more complicated.
     */
    private static void insertAtHalf(byte[] arr, int x, int y, int z, int val) {
        int pos = getBlockIndex(x, y, z);
        boolean isUpperHalf = pos % 2 == 0;
        if (!isUpperHalf) {
            arr[pos / 2] |= (val << 4) & 0xF0;
        } else {
            arr[pos / 2] |= val & 0x0F;
        }
    }

    /**
     * In 1.14, we ignore the damage value and just get the block ID
     * @param state the block state - ID and 4 bits of data
     * @return the block ID
     */
    @Override
    protected int transformState(int state) { return state >> 4; }
}
