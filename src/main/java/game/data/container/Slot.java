package game.data.container;

import game.data.WorldManager;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

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

    public CompoundTag toNbt(int index) {
        CompoundTag tag = new CompoundTag();
        tag.add("Count", new ByteTag(count));
        tag.add("id", new StringTag(WorldManager.getItemRegistry().getItemName(itemId)));
        tag.add("Slot", new ByteTag(index));
        return tag;
    }


}
