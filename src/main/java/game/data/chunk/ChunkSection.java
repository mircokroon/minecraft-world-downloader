package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;

/**
 * Class to hold a 16 block tall chunk section.
 */
public abstract class ChunkSection {
    protected long[] blocks;
    protected byte[] blockLight;
    protected byte[] skyLight;
    protected byte y;
    protected Palette palette;

    public ChunkSection(byte y, Palette palette) {
        this.y = y;
        this.blockLight = new byte[2048];
        this.skyLight = new byte[2048];
        this.palette = palette;
    }

    public void setSkyLight(byte[] skyLight) {
        this.skyLight = skyLight;
    }

    public void setBlockLight(byte[] blockLight) {
        this.blockLight = blockLight;
    }

    public void setBlocks(long[] blocks) {
        this.blocks = blocks;
    }

    /**
     * Convert this section to NBT.
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.add("Y", new ByteTag(y));


        if (blockLight != null && blockLight.length != 0) {
            tag.add("BlockLight", new ByteArrayTag(blockLight));
        }

        if (skyLight != null && skyLight.length != 0) {
            tag.add("SkyLight", new ByteArrayTag(skyLight));
        }

        addNbtTags(tag);

        return tag;
    }

    protected abstract void addNbtTags(CompoundTag map);

    public static int getBlockIndex(int x, int y, int z) {
        return y * 16 * 16 + z * 16 + x;
    }

    public byte getY() {
        return y;
    }

    public BlockState topmostBlockStateAt(int x, int z) {
        for (int y = 15; y >= 0 ; y--) {
            int index = getPaletteIndex(palette.bitsPerBlock, x, y, z);
            int state = palette.stateFromId(index);
            if (state == 0) { continue;}

            BlockState blockState = WorldManager.getGlobalPalette().getState(state >> 4);
            if (blockState == null) { continue; }

            return blockState;
        }
        return null;
    }

    protected int getPaletteIndex(int bitsPerBlock, int x, int y, int z) {
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
