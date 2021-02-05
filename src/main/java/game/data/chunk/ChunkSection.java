package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import packets.builder.PacketBuilder;
import packets.lib.ByteQueue;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.Tag;

import java.util.Arrays;
import java.util.Objects;

/**
 * Class to hold a 16 block tall chunk section.
 */
public abstract class ChunkSection {
    protected long[] blocks;
    protected byte[] blockLight;
    protected byte[] skyLight;
    protected byte y;
    protected Palette palette;

    public abstract int getDataVersion();

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

            BlockState state = GlobalPaletteProvider.getGlobalPalette(getDataVersion()).getState(blockStateId);

            if (state == null || !state.isSolid()) {
                continue;
            }
            return y;
        }
        return -1;
    }

    public int getNumericBlockStateAt(int x, int y, int z) {
        return palette.stateFromId(getPaletteIndex(x, y, z));
    }

    public int getPaletteIndex(int x, int y, int z) {
        return getPaletteIndex(x, y, z, palette.getBitsPerBlock());
    }

    public int getPaletteIndex(int x, int y, int z, int bitsPerBlock) {
        if (blocks.length == 0) {
            return 0;
        }

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

    public void write(PacketBuilder packet) {
        packet.writeShort(4096);

        palette.write(packet);

        packet.writeVarInt(blocks.length);
        packet.writeLongArray(blocks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkSection that = (ChunkSection) o;

        if (y != that.y) return false;
        if (!Arrays.equals(blocks, that.blocks)) return false;
        if (!Arrays.equals(blockLight, that.blockLight)) return false;
        if (!Arrays.equals(skyLight, that.skyLight)) return false;
        return Objects.equals(palette, that.palette);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(blocks);
        result = 31 * result + (int) y;
        result = 31 * result + (palette != null ? palette.hashCode() : 0);
        return result;
    }
}

