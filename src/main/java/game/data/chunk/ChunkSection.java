package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.Tag;

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

    public ChunkSection(int sectionY, Tag nbt) {
        this.y = (byte) sectionY;
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

    public int height(int x, int z) {
        for (int y = 15; y >= 0 ; y--) {
            int blockStateId = getNumericBlockStateAt(x, y, z);

            BlockState state = WorldManager.getGlobalPalette().getState(blockStateId);
            if (state == null || !state.isSolid()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    public int getNumericBlockStateAt(int x, int y, int z) {
        int state = palette.stateFromId(palette.getIndex(blocks, x, y, z));

        return transformState(state);
    }

    protected int transformState(int state) { return state; }
}
