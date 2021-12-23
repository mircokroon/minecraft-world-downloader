package game.data.chunk.palette;

import se.llbit.nbt.*;

import java.util.List;

public class SingleValuePalette extends Palette {
    int val;
    public SingleValuePalette(int val) {
        this.val = val;
    }

    @Override
    public boolean hasData() {
        return false;
    }

    @Override
    public String toString() {
        return "SingleValuePalette{" +
                "val=" + val +
                '}';
    }

    @Override
    public int stateFromId(int index) {
        return val;
    }

    @Override
    public List<SpecificTag> toNbt() {
        return List.of(stateProvider.getState(val).toNbt());
    }
}
