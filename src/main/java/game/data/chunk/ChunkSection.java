package game.data.chunk;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;

import packets.builder.PacketBuilder;

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
        CompoundMap map = new CompoundMap();
        map.put(new ByteTag("Y", y));


        if (blockLight != null && blockLight.length != 0) {
            map.put(new ByteArrayTag("BlockLight", blockLight));
        }

        if (skyLight != null && skyLight.length != 0) {
            map.put(new ByteArrayTag("SkyLight", skyLight));
        }

        addNbtTags(map);

        return new CompoundTag("", map);
    }

    protected abstract void addNbtTags(CompoundMap map);

    public static int getBlockIndex(int x, int y, int z) {
        return y * 16 * 16 + z * 16 + x;
    }

    public byte getY() {
        return y;
    }
}
