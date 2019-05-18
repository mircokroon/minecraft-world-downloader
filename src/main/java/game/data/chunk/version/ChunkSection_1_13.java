package game.data.chunk.version;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;

import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;

public class ChunkSection_1_13 extends ChunkSection {

    protected long[] blocks;
    public ChunkSection_1_13(byte y, Palette palette) {
        super(y, palette);
    }

    @Override
    protected void addNbtTags(CompoundMap map) {
        // TODO: handle making of the palette & inserting BlockStates(x256L)
    }

    private ListTag<CompoundTag> createPalette() {
        return new ListTag<>("Palette", CompoundTag.class, palette.toNbt());
    }
}
