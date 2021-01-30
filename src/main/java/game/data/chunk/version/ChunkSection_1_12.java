package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.DummyPalette;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.Tag;

/**
 * Chunk sections in 1.12 require parsing of the full block data as the level format does not include a palette
 * as the new versions do. As such this class is more lengthy than the new versions.
 */
public class ChunkSection_1_12 extends ChunkSection {
    protected int[][][] blockStates;

    @Override
    public int getDataVersion() {
        return Chunk_1_12.DATA_VERSION;
    }

    public ChunkSection_1_12(byte y, Palette palette) {
        super(y, palette);
        this.blockStates = new int[16][16][16];
    }

    public ChunkSection_1_12(int sectionY, Tag nbt) {
        super(sectionY, nbt);
        this.blockStates = new int[16][16][16];
        this.palette = new DummyPalette();

        long[] blocks = parseBlockIds(nbt.get("Blocks").byteArray());
        parseBlockStates(nbt.get("Data").byteArray(), blocks);
        setBlocks(blocks);

    }

    @Override
    public void setBlocks(long[] blocks) {
        super.setBlocks(blocks);
        for (int y = 0; y < Chunk.SECTION_HEIGHT; y++) {
            for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {
                for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
                    int data = getPaletteIndex(x, y, z);

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
                    insertAtHalf(blockData, x, y, z, blockStates[x][y][z] & 0x0F);
                }
            }
        }
        return blockData;
    }

    private long[] parseBlockIds(byte[] arr) {
        long[] blocks = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            blocks[i] = (arr[i] & 0xFF) << 4;
        }
        return blocks;
    }

    private void parseBlockStates(byte[] arr, long[] blocks) {
        for (int i = 0; i < blocks.length; i++) {
            int b = arr[i/2] & 0xFF;
            if (i % 2 == 0) {
                b = b & 0x0F;
            } else {
                b = (b & 0xF0) >> 4;
            }
            blocks[i] |= b;
        }
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
}
