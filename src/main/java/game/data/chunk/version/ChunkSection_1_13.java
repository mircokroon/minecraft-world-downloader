package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongArrayTag;
import se.llbit.nbt.Tag;

/**
 * Starting with 1.13, the chunk format requires a palette and the palette indices. This is actually
 * much easier for us as the packet also comes in palette indices, so we can just copy those over and
 * convert the palette from the packet to an NBT palette.
 */
public class ChunkSection_1_13 extends ChunkSection {

    @Override
    public int getDataVersion() {
        return Chunk_1_13.DATA_VERSION;
    }

    public ChunkSection_1_13(byte y, Palette palette) {
        super(y, palette);
    }
    public ChunkSection_1_13(int sectionY, Tag nbt) {
        super(sectionY, nbt);
        this.setBlocks(nbt.get("BlockStates").longArray());
        this.palette = new Palette(getDataVersion(), nbt.get("Palette").asList());
    }

    @Override
    protected void addNbtTags(CompoundTag map) {
        map.add("BlockStates", new LongArrayTag(blocks));
        map.add("Palette", createPalette());
    }

    private ListTag createPalette() {
        return new ListTag(Tag.TAG_COMPOUND, palette.toNbt());
    }
}
