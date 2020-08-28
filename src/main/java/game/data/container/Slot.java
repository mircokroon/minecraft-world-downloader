package game.data.container;

import se.llbit.nbt.SpecificTag;

public class Slot {
    int itemId;
    int count;
    SpecificTag nbt;

    public Slot(int itemId, byte count, SpecificTag nbt) {
        this.itemId = itemId;
        this.count = count;
        this.nbt = nbt;
    }

    @Override
    public String toString() {
        return "Slot{" +
            "itemId=" + itemId +
            ", count=" + count +
            ", nbt=" + nbt +
            '}';
    }
}
