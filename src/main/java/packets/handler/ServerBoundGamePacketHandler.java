package packets.handler;

import config.Version;
import java.util.HashMap;
import java.util.Map;

import config.Config;
import game.data.WorldManager;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDouble3D;
import proxy.ConnectionManager;

public class ServerBoundGamePacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundGamePacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        PacketOperator updatePlayerPosition = provider -> {
            double x = provider.readDouble();
            double y = provider.readDouble();
            double z = provider.readDouble();

            WorldManager.getInstance().setPlayerPosition(x, y, z);

            return true;
        };

        PacketOperator updatePlayerRotation = provider -> {
            double yaw = provider.readFloat() % 360;
            WorldManager.getInstance().setPlayerRotation(yaw);
            return true;
        };

        operations.put("MovePlayerPos", updatePlayerPosition);
        operations.put("MovePlayerRot", updatePlayerRotation);
        operations.put("MovePlayerPosRot", (provider) -> {
            updatePlayerPosition.apply(provider);
            updatePlayerRotation.apply(provider);
            return true;
        });

        operations.put("MoveVehicle", updatePlayerPosition);

        operations.put("UseItem", provider -> {
            // newer versions first include a VarInt with the hand
            if (Config.versionReporter().isAtLeast(Version.V1_14)) {
                provider.readVarInt();
            }

            return true;
        });

        operations.put("ContainerClose", provider -> {
            final byte windowId = provider.readNext();
            WorldManager.getInstance().getContainerManager().closeWindow(windowId);
            WorldManager.getInstance().getVillagerManager().closeWindow(windowId);
            return true;
        });

        // block placements
        operations.put("UseItemOn", provider -> {
            provider.readVarInt();  // Hand
            Coordinate3D coords = provider.readCoordinates();
            provider.readVarInt();  // Block face
            provider.readFloat();   // Cursor x
            provider.readFloat();   // Cursor y
            provider.readFloat();   // Cursor z
            provider.readBoolean(); // If the player's head is inside of a block

            WorldManager.getInstance().getContainerManager().lastInteractedWith(coords);
            return true;
        });

        operations.put("SetCommandBlock", provider -> {
            WorldManager.getInstance().getCommandBlockManager().readAndStoreCommandBlock(provider);
            return true;
        });

        operations.put("Interact", provider -> {
            WorldManager.getInstance().getVillagerManager().lastInteractedWith(provider);
            return true;
        });
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return false;
    }
}

