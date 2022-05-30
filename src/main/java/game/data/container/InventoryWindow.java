package game.data.container;

import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.WorldManager;
import se.llbit.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class InventoryWindow {
    private int windowType;
    private String windowTitle;

    private int slotCount;
    private List<Slot> slotList;

    Coordinate3D containerLocation;

    /**
     * Constructor for 1.12, slot count is just given here
     */
    InventoryWindow(String windowTitle, Coordinate3D containerLocation, int slotCount) {
        this.windowType = -1;
        this.windowTitle = windowTitle;
        this.containerLocation = containerLocation;

        this.slotCount = slotCount;
    }

    /**
     * Constructor for later versions, we need to check the window type against the registry
     */
    InventoryWindow(int windowType, String windowTitle, Coordinate3D containerLocation) {
        this.windowType = windowType;
        this.windowTitle = windowTitle;
        this.containerLocation = containerLocation;

        this.slotCount = WorldManager.getInstance().getMenuRegistry().getSlotCount(windowType);
    }

    private InventoryWindow(InventoryWindow other) {
        this.windowType = other.windowType;
        this.windowTitle = other.windowTitle;
        this.containerLocation = other.containerLocation;
        this.slotCount = other.slotCount;
    }

    // use the slot count to avoid adding items from the player's inventory
    public void setSlots(List<Slot> slots) {
        slotList = slots.subList(0, slotCount);
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public Coordinate3D getContainerLocation() {
        return containerLocation;
    }

    public void adjustContainerLocation(Coordinate2D change) {
        this.containerLocation = this.containerLocation.add(change);
    }

    public List<Slot> getSlotList() {
        return slotList;
    }

    public int getType() {
        return windowType;
    }

    public InventoryWindow[] split() {
        InventoryWindow first = new InventoryWindow(this);
        InventoryWindow second = new InventoryWindow(this);

        first.slotList = slotList.subList(0, slotCount / 2);
        second.slotList = slotList.subList(slotCount / 2, slotCount);

        return new InventoryWindow[]{first, second};
    }

    public List<CompoundTag> getSlotsNbt() {
        List<CompoundTag> result = new ArrayList<>(slotList.size());
        for (int i = 0; i < slotList.size(); i++) {
            Slot slot = slotList.get(i);
            if (slot == null) { continue; }

            result.add(slot.toNbt(i));
        }
        return result;
    }

    public int getSlotCount() {
        return slotCount;
    }

    public boolean hasCustomName() {
        return !windowTitle.startsWith("{\"translate");
    }

    @Override
    public String toString() {
        return "InventoryWindow{" +
                "windowType=" + windowType +
                ", windowTitle='" + windowTitle + '\'' +
                ", slotCount=" + slotCount +
                ", slotList=" + slotList +
                ", containerLocation=" + containerLocation +
                '}';
    }
}
