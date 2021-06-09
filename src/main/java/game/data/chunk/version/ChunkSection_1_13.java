package game.data.chunk.version;

import config.Version;
import game.data.WorldManager;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.dimension.Dimension;
import packets.builder.PacketBuilder;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongArrayTag;
import se.llbit.nbt.Tag;

import java.util.Arrays;

/**
 * Starting with 1.13, the chunk format requires a palette and the palette indices. This is actually
 * much easier for us as the packet also comes in palette indices, so we can just copy those over and
 * convert the palette from the packet to an NBT palette.
 */
public class ChunkSection_1_13 extends ChunkSection {
    public static final Version VERSION = Version.V1_13;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    public ChunkSection_1_13(byte y, Palette palette) {
        super(y, palette);
    }
    public ChunkSection_1_13(int sectionY, Tag nbt) {
        super(sectionY, nbt);
        this.setBlocks(nbt.get("BlockStates").longArray());
        this.setBlockLight(nbt.get("BlockLight").byteArray());
        this.setSkyLight(nbt.get("SkyLight").byteArray());
        this.palette = new Palette(getDataVersion(), nbt.get("Palette").asList());
    }

    @Override
    public void write(PacketBuilder packet) {
        palette.write(packet);

        packet.writeVarInt(blocks.length);
        packet.writeLongArray(blocks);

        packet.writeByteArray(this.blockLight);

        if (WorldManager.getInstance().getDimension() != Dimension.NETHER) {
            packet.writeByteArray(this.skyLight);
        }
    }

    @Override
    protected void addNbtTags(CompoundTag map) {
        map.add("BlockStates", new LongArrayTag(blocks));
        map.add("Palette", createPalette());
    }

    private ListTag createPalette() {
        return new ListTag(Tag.TAG_COMPOUND, palette.toNbt());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkSection_1_13 that = (ChunkSection_1_13) o;

        if (getY() != that.getY()) return false;
        return Arrays.equals(blocks, that.blocks);
    }
}
