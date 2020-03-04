package game.data.chunk.palette;

import se.llbit.nbt.SpecificTag;

import java.util.List;

public class DummyPalette extends Palette {

    public DummyPalette() {
        super();
    }

    @Override
    public int stateFromId(int index) {
        return index;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<SpecificTag> toNbt() {
        throw new UnsupportedOperationException("Cannot convert a dummy palette to NBT");
    }

    @Override
    public int getIndex(long[] blocks, int x, int y, int z) {
        return (int) blocks[y * 16 * 16 + z * 16 + x];
    }
}
