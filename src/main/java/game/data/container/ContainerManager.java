package game.data.container;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.palette.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerManager {
    private static final int PLAYER_INVENTORY = 0;

    private Coordinate3D lastInteractedWith;
    private Map<Integer, InventoryWindow> knownWindows;

    public ContainerManager() {
        knownWindows = new HashMap<>();
    }

    public void lastInteractedWith(Coordinate3D coordinates) {
        lastInteractedWith = coordinates;
    }

    public void openWindow(int windowId, int windowType, String windowTitle) {
        if (windowId == PLAYER_INVENTORY) {
            return;
        }

        if (lastInteractedWith != null) {
            InventoryWindow window = new InventoryWindow(windowType, windowTitle, lastInteractedWith);

            // if a window has 0 slots, ignore it
            if (window.getSlotCount() > 0) {
                knownWindows.put(windowId, window);
            }
        }
    }

    public void closeWindow(int windowId) {
        if (!knownWindows.containsKey(windowId)) { return; }

        InventoryWindow window = knownWindows.remove(windowId);
        closeWindow(window);
    }

    private void closeWindow(InventoryWindow window) {
        Chunk c = WorldManager.getChunk(window.containerLocation.globalToChunk().addDimension(Game.getDimension()));

        if (c == null) { return; }

        BlockState block = c.getBlockStateAt(window.getContainerLocation().withinChunk());
        if (block == null) { return; }

        WorldManager.touchChunk(c);

        if (window.getSlotList().size() == 54 && block.isDoubleChest()) {
            addDoubleChestInventory(block, window);
        } else {
            c.addInventory(window);
        }
    }

    private void addDoubleChestInventory(BlockState block, InventoryWindow window) {
        InventoryWindow[] chests = window.split();
        Coordinate2D companionDirection = getCompanionChestDirection(block);

        int adjustPositionOf = block.getProperty("type").equals("left") ? 0 : 1;
        chests[adjustPositionOf].adjustContainerLocation(companionDirection);

        closeWindow(chests[0]);
        closeWindow(chests[1]);
    }

    private Coordinate2D getCompanionChestDirection(BlockState block) {
        Direction direction = Direction.valueOf(block.getProperty("facing").toUpperCase());

        if (block.getProperty("type").equals("left")) {
            return direction.clockwise().toCoordinate();
        } else {
            return direction.counterClockwise().toCoordinate();
        }
    }

    public void items(int windowId, List<Slot> slots) {
        InventoryWindow window = knownWindows.get(windowId);

        if (window != null) {
            window.setSlots(slots);
        }
    }
}

