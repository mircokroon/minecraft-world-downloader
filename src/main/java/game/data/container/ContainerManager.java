package game.data.container;

import config.Config;
import game.data.chunk.ChunkEntities;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.palette.BlockState;
import org.apache.commons.lang3.ArrayUtils;
import packets.DataTypeProvider;
import packets.builder.Chat;
import packets.builder.MessageTarget;
import packets.builder.PacketBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerManager {
    private static final int PLAYER_INVENTORY = 0;

    private Coordinate3D lastInteractedWith;
    private final Map<Integer, InventoryWindow> knownWindows;
    private final Map<CoordinateDim3D, InventoryWindow> storedWindows;

    public ContainerManager() {
        knownWindows = new HashMap<>();
        storedWindows = new HashMap<>();
    }

    public void lastInteractedWith(Coordinate3D coordinates) {
        lastInteractedWith = coordinates;
    }

    public void openWindow_1_12(int windowId, int numSlots, String windowTitle) {
        if (windowId == PLAYER_INVENTORY) {
            return;
        }

        if (lastInteractedWith != null) {
            InventoryWindow window = new InventoryWindow(windowTitle, lastInteractedWith, numSlots);

            // if a window has 0 slots, ignore it
            if (window.getSlotCount() > 0) {
                knownWindows.put(windowId, window);
            }
        }
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
        if (window.getSlotList() == null) { return; }

        Chunk c = WorldManager.getInstance().getChunk(window.containerLocation.globalToChunk().addDimension(WorldManager.getInstance().getDimension()));

        if (c == null) {
            sendInventoryFailureMessage(window, "Chunk not loaded.");
            return;
        }

        BlockState block = c.getBlockStateAt(window.getContainerLocation().withinChunk());

        if (block == null) {
            sendInventoryFailureMessage(window, "No block found.");
            return;
        }

        WorldManager.getInstance().touchChunk(c);

        if (window.getSlotList().size() == 54 && block.hasProperty("type") && block.isDoubleChest()) {
            addDoubleChestInventory(block, window);
        } else if (window.getSlotList().size() == 54 && !block.hasProperty("type") && block.isChest()) {
            handleChest1_12(block, window);
        } else {
            c.addInventory(window, true);
            storedWindows.put(window.containerLocation.addDimension3D(WorldManager.getInstance().getDimension()), window);
        }
    }

    private void sendInventoryFailureMessage(InventoryWindow window, String cause) {
        if (Config.sendInfoMessages()) {
            Chat message = new Chat("Unable to save inventory at " + window.getContainerLocation() + ". " + cause);
            message.setColor("red");

            Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }


    /**
     * Handles double chests in 1.12.
     */
    private void handleChest1_12(BlockState block, InventoryWindow window) {
        Direction facing = Direction.valueOf(block.getProperty("facing").toUpperCase());
        Coordinate3D pos = window.getContainerLocation();

        Coordinate3D beforePos = pos.add(facing.clockwise().toCoordinate());
        BlockState blockBefore = WorldManager.getInstance().blockStateAt(beforePos);

        InventoryWindow[] chests = window.split();

        // for some reason the ordering of double chests depends on the direction they are facing in 1.12 (wtf?)
        if (facing.equals(Direction.NORTH) || facing.equals(Direction.EAST)) {
            ArrayUtils.swap(chests, 0, 1);
        }

        // if it's the left half of the chest
        if (blockBefore == block) {
            chests[0].adjustContainerLocation(facing.clockwise().toCoordinate());
        } else {
            // otherwise it must be the right half ... we don't support triple chests
            chests[1].adjustContainerLocation(facing.counterClockwise().toCoordinate());
        }


        closeWindow(chests[0]);
        closeWindow(chests[1]);
    }

    /**
     * Split Window into two halves, for two halves of a double chest.
     */
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

    public void items(int windowId, int count, DataTypeProvider provider) {
        InventoryWindow window = knownWindows.get(windowId);

        if (window != null) {
            List<Slot> slots = provider.readSlots(count);

            window.setSlots(slots);
        }
    }

    public void loadPreviousInventoriesAt(ChunkEntities c, CoordinateDim3D location) {
        if (storedWindows.containsKey(location)) {
            c.addInventory(storedWindows.get(location), false);
        }

    }
}

