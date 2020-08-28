package game.data.container;

import game.data.Coordinate3D;
import game.data.WorldManager;
import se.llbit.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class InventoryWindow {
    int windowType;
    String windowTitle;
    Coordinate3D containerLocation;

    int slotCount;
    List<Slot> slotList;
    public InventoryWindow(int windowType, String windowTitle, Coordinate3D containerLocation) {
        this.windowType = windowType;
        this.windowTitle = windowTitle;
        this.containerLocation = containerLocation;

        this.slotCount = WorldManager.getMenuRegistry().getSlotCount(windowType);
    }

    public void setSlots(List<Slot> slots) {
        slotList = slots.subList(0, slotCount);
        System.out.println("Inventory at " + containerLocation + " now has " + slotList.size() + " items.");
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public Coordinate3D getContainerLocation() {
        return containerLocation;
    }

    public List<Slot> getSlotList() {
        return slotList;
    }

    public List<CompoundTag> getSlotsNbt() {
        return getSlotsNbt(0, slotCount);
    }

    public List<CompoundTag> getSlotsNbt(int from, int to) {
        List<CompoundTag> result = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            Slot slot = slotList.get(i);
            if (slot == null) { continue; }

            result.add(slot.toNbt(i - from));
        }
        return result;
    }
}
