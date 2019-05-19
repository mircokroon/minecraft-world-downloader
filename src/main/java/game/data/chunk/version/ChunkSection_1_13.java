package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongArrayTag;
import se.llbit.nbt.Tag;

public class ChunkSection_1_13 extends ChunkSection {

    public ChunkSection_1_13(byte y, Palette palette) {
        super(y, palette);
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
