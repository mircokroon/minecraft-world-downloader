package game.data.container;

import game.data.WorldManager;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

public class Slot {
    private int itemId;
    private int count;
    private SpecificTag nbt;

    public Slot(int itemId, byte count, SpecificTag nbt) {
        this.itemId = itemId;
        this.count = count;
        this.nbt = nbt;
    }

    @Override
    public String toString() {
        return "Slot{" +
            "itemId=" + itemId +
            ", Name=" + WorldManager.getInstance().getItemRegistry().getItemName(itemId) +
            ", count=" + count +
            ", nbt=" + nbt +
            '}';
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.add("id", new StringTag(WorldManager.getInstance().getItemRegistry().getItemName(itemId)));
        tag.add("Count", new ByteTag(count));

        if (nbt instanceof CompoundTag) {
            tag.add("tag", nbt);
        }
        return tag;
    }

    public CompoundTag toNbt(int index) {
        CompoundTag tag = toNbt();
        tag.add("Slot", new ByteTag(index));
        return tag;
    }


}
