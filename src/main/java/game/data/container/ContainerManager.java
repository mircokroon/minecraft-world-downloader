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
        if (window.getSlotList() == null) { return; }

        closeWindow(window);

        // Always confirm — the inventory is remembered and will be written even if the chunk loads later.
        sendInventorySavedMessage(window);
    }

    private void closeWindow(InventoryWindow window) {
        if (window.getSlotList() == null) { return; }

        Chunk c = WorldManager.getInstance().getChunk(window.containerLocation.globalToChunk().addDimension(WorldManager.getInstance().getDimension()));
        BlockState block = (c != null) ? c.getBlockStateAt(window.getContainerLocation().withinChunk()) : null;

        // Double chests: split into two halves (each half is stored/applied via the recursive call).
        if (c != null && block != null && window.getSlotList().size() == 54 && block.hasProperty("type") && block.isDoubleChest()) {
            WorldManager.getInstance().touchChunk(c);
            addDoubleChestInventory(block, window);
            return;
        }
        if (c != null && block != null && window.getSlotList().size() == 54 && !block.hasProperty("type") && block.isChest()) {
            WorldManager.getInstance().touchChunk(c);
            handleChest1_12(block, window);
            return;
        }

        // Always remember the inventory so it survives chunk re-sends / block updates and is (re)applied
        // whenever this chunk is (re)loaded — even if the chunk isn't loaded right now.
        storedWindows.put(window.containerLocation.addDimension3D(WorldManager.getInstance().getDimension()), window);

        if (c != null && block != null) {
            WorldManager.getInstance().touchChunk(c);
            c.addInventory(window, false);
        }
    }

    private void sendInventorySavedMessage(InventoryWindow window) {
        if (Config.sendInfoMessages()) {
            Chat message = new Chat("Hui Downloader saved inventory at " + window.getContainerLocation());
            message.setColor("green");
            // Show only on the action bar (above the hotbar), not in the chat box.
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

