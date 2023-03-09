package game.data.chunk.version;

import config.Version;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.DirectPalette;
import game.data.chunk.palette.Palette;
import game.data.coordinates.Coordinate3D;
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
    public ChunkSection_1_13(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }
    public ChunkSection_1_13(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, chunk);
        parse(nbt);
    }

    protected void parse(Tag nbt) {
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
        if (palette instanceof DirectPalette) {
            convertToIndirectPalette();
        }
        
        map.add("BlockStates", new LongArrayTag(blocks));
        map.add("Palette", createPalette());
    }

    /**
     * Before saving chunks, we need to ensure that it's correctly using a palette. Chunk sections with a large variety of
     * blocks need to be converted.
     */
    private void convertToIndirectPalette() {
        ChunkSection newSection = this.chunk.createNewChunkSection(this.y, Palette.empty());
        newSection.setBlocks(new long[256]);

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    newSection.setBlockAt(new Coordinate3D(x, y, z), getNumericBlockStateAt(x, y, z));
                }
            }
        }

        newSection.copyTo(this);
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
