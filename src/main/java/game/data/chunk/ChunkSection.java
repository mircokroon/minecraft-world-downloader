package game.data.chunk;

import game.data.chunk.palette.BlockRegistry;
import java.util.Arrays;

import org.apache.commons.lang3.mutable.MutableBoolean;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import game.data.coordinates.Coordinate3D;
import packets.builder.PacketBuilder;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;

/**
 * Class to hold a 16 block tall chunk section.
 */
public abstract class ChunkSection {
    protected final Chunk chunk;

    protected long[] blocks;
    protected byte[] blockLight;
    protected byte[] skyLight;
    protected byte y;
    protected Palette palette;

    BlockLocationEncoder locationHelper = new BlockLocationEncoder();

    public int getDataVersion() {
        return chunk.getDataVersion();
    }

    public ChunkSection(byte y, Palette palette, Chunk chunk) {
        this.chunk = chunk;
        this.y = y;
        this.palette = palette;
    }

    protected BlockLocationEncoder getLocationEncoder() {
        return this.locationHelper;
    }

    public ChunkSection(int sectionY, Chunk chunk) {
        this.y = (byte) sectionY;
        this.chunk = chunk;
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

    public int computeHeight(int x, int z, MutableBoolean foundAir) {
        BlockRegistry globalPalette = GlobalPaletteProvider.getGlobalPalette(getDataVersion());

        for (int y = 15; y >= 0 ; y--) {
            int blockStateId = getNumericBlockStateAt(x, y, z);

            BlockState state = globalPalette.getState(blockStateId);

            if (state == null || !state.isSolid()) {
                foundAir.setTrue();
                continue;
            }

            if (foundAir.isFalse()) {
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

    private synchronized int getPaletteIndex(int x, int y, int z, int bitsPerBlock) {
        if (blocks.length == 0 || bitsPerBlock == 0) {
            return 0;
        }

        return getLocationEncoder().setTo(x, y, z, bitsPerBlock).fetch(blocks);
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
        if (!Arrays.equals(blockLight, that.blockLight)) return false;
        return Arrays.equals(skyLight, that.skyLight);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(blocks);
        result = 31 * result + (int) y;
        result = 31 * result + (palette != null ? palette.hashCode() : 0);
        return result;
    }

    public synchronized void setBlockAt(Coordinate3D coords, int blockStateId) {
        int index = palette.getIndexFor(this, blockStateId);

        getLocationEncoder().setTo(
                coords.getX(), coords.getY(), coords.getZ(),
                palette.getBitsPerBlock()
        );
        getLocationEncoder().write(blocks, index);
        WorldManager.getInstance().touchChunk(chunk);
    }

    /**
     * When the bits per block increases, we must rewrite the blocks array.
     */
    public synchronized void resizeBlocksIfRequired(int newBitsPerBlock) {
        int newSize = newBitsPerBlock * 64;
        long[] newBlocks = new long[newSize];

        if (blocks == null) {
            this.blocks = newBlocks;
            return;
        }

        copyBlocks(newBlocks, newBitsPerBlock);
    }

    public synchronized void copyBlocks(long[] newBlocks, int newBitsPerBlock) {
        BlockLocationEncoder locationHelper = getLocationEncoder();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int index = getPaletteIndex(x, y, z);

                    locationHelper.setTo(x, y, z, newBitsPerBlock).write(newBlocks, index);
                }
            }
        }
        this.blocks = newBlocks;
    }

    public byte[] getSkyLight() { return skyLight; }
    public byte[] getBlockLight() { return blockLight; }

    public void resetBlocks() {
        this.blocks = new long[256];
        this.palette = Palette.empty();
    }

    public void copyTo(ChunkSection other) {
        other.blocks = this.blocks;
        other.palette = this.palette;
    }

    @Override
    public String toString() {
        return "ChunkSection{" +
            "blocks=[" + blocks.length +
            "], y=" + y +
            ", palette=" + palette +
            '}';
    }
}

