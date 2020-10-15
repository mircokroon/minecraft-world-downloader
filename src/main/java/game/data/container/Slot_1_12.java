package game.data.container;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ShortTag;
import se.llbit.nbt.SpecificTag;

public class Slot_1_12 extends Slot {
    private int damage;

    public Slot_1_12(int itemId, byte count, int damage, SpecificTag nbt) {
        super(itemId, count, nbt);

        this.damage = damage;
    }

    @Override
    public String toString() {
        return "Slot_1_12{" +
            super.toString() +
            ", damage=" + damage +
            '}';
    }

    public CompoundTag toNbt(int index) {
        CompoundTag tag = super.toNbt(index);
        tag.add("Damage", new ShortTag((short) damage));
        return tag;
    }
}
