package game.data.chunk;

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
}
