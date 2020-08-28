package game.data.container;

import game.Game;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerManager {
    static final int PLAYER_INVENTORY = 0;

    Coordinate3D lastInteractedWith;
    Map<Integer, InventoryWindow> knownWindows;

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
            knownWindows.put(windowId, new InventoryWindow(windowType, windowTitle, lastInteractedWith));
        }
    }

    public void closeWindow(int windowId) {
        System.out.println("Saving items for window " + knownWindows.get(windowId));

        if (!knownWindows.containsKey(windowId)) {
            return;
        }

        InventoryWindow window = knownWindows.remove(windowId);
        Chunk c = WorldManager.getChunk(window.containerLocation.chunkPos().addDimension(Game.getDimension()));

        if (c != null) {
            c.addInventory(window);
        }
    }

    public void items(int windowId, List<Slot> slots) {
        System.out.println("Window ID: " + windowId + ". >> " + knownWindows);
        InventoryWindow window = knownWindows.get(windowId);

        if (window != null) {
            window.setSlots(slots);
        } else {
            System.out.println("Items given for unknown inventory");
        }
    }
}

