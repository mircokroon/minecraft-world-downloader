package game.data.container;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds the contents of the player's own inventory (container window 0) so they can be written into
 * level.dat under Data.Player.Inventory when the world is saved.
 *
 * Contents are kept up to date from two packets: the full inventory sync ({@link #setSlots}) and
 * single-slot updates ({@link #setSlot}). The slot indices used in the inventory window are not the
 * same as the slot numbers Minecraft expects in level.dat, so {@link #toNbt()} maps between the two.
 */
public class PlayerInventory {
    // Layout of the player inventory window (window id 0), 1.9+:
    //    0      crafting result
    //    1 - 4  crafting grid
    //    5 - 8  armor: helmet, chestplate, leggings, boots
    //    9 - 35 main inventory
    //   36 - 44 hotbar
    //   45      offhand
    private static final int ARMOR_START = 5;
    private static final int ARMOR_END = 8;
    private static final int MAIN_START = 9;
    private static final int MAIN_END = 35;
    private static final int HOTBAR_START = 36;
    private static final int HOTBAR_END = 44;
    private static final int OFFHAND = 45;

    // level.dat slot number for the offhand and the topmost armor slot (helmet)
    private static final int OFFHAND_SLOT = -106;
    private static final int HELMET_SLOT = 103;

    // current contents, keyed by window-0 slot index (empty slots are absent)
    private final Map<Integer, Slot> windowSlots = new TreeMap<>();

    /**
     * Replaces the whole inventory from a full window-0 content packet. The list is indexed by
     * window slot, with null entries for empty slots.
     */
    public void setSlots(List<Slot> slots) {
        windowSlots.clear();
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot != null) {
                windowSlots.put(i, slot);
            }
        }
    }

    /**
     * Updates a single window-0 slot (a null slot clears it).
     */
    public void setSlot(int windowSlot, Slot slot) {
        if (slot == null) {
            windowSlots.remove(windowSlot);
        } else {
            windowSlots.put(windowSlot, slot);
        }
    }

    public boolean hasItems() {
        return !windowSlots.isEmpty();
    }

    /**
     * Maps a window-0 slot index to the slot number used in level.dat's Player.Inventory list, or
     * returns null if the slot should not be saved (the crafting result and crafting grid).
     */
    private static Integer toLevelDatSlot(int windowSlot) {
        if (windowSlot >= MAIN_START && windowSlot <= MAIN_END) {
            // main inventory uses the same numbering in level.dat
            return windowSlot;
        }
        if (windowSlot >= HOTBAR_START && windowSlot <= HOTBAR_END) {
            // hotbar is slots 0 - 8 in level.dat
            return windowSlot - HOTBAR_START;
        }
        if (windowSlot >= ARMOR_START && windowSlot <= ARMOR_END) {
            // window order is helmet, chestplate, leggings, boots (5..8);
            // level.dat uses head=103, chest=102, legs=101, feet=100
            return HELMET_SLOT - (windowSlot - ARMOR_START);
        }
        if (windowSlot == OFFHAND) {
            return OFFHAND_SLOT;
        }
        // crafting result / crafting grid: not part of the saved inventory
        return null;
    }

    /**
     * Builds the ListTag written to Data.Player.Inventory.
     */
    public ListTag toNbt() {
        List<CompoundTag> items = new ArrayList<>();
        for (Map.Entry<Integer, Slot> entry : windowSlots.entrySet()) {
            Integer levelSlot = toLevelDatSlot(entry.getKey());
            if (levelSlot == null) {
                continue;
            }

            items.add(entry.getValue().toNbt(levelSlot));
        }
        return new ListTag(Tag.TAG_COMPOUND, items);
    }
}
