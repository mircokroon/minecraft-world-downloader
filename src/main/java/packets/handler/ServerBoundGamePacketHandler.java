package packets.handler;

import config.Config;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.WorldManager;
import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundGamePacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundGamePacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        PacketOperator updatePlayerPosition = provider -> {
            double x = provider.readDouble();
            double y = provider.readDouble();
            double z = provider.readDouble();

            CoordinateDouble3D playerPos = new CoordinateDouble3D(x, y, z);
            WorldManager.getInstance().setPlayerPosition(playerPos);

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
            if (Config.versionReporter().isAtLeast1_14()) {
                provider.readVarInt();
            }

            return true;
        });

        operations.put("ContainerClose", provider -> {
            WorldManager.getInstance().getContainerManager().closeWindow(provider.readNext());
            return true;
        });

        operations.put("PlayerBlockPlacement", provider -> {
            provider.readVarInt();  // Hand
            Coordinate3D coords = provider.readCoordinates(); // Position
            provider.readVarInt();  // Block face
            provider.readFloat();   // Cursor x
            provider.readFloat();   // Cursor y
            provider.readFloat();   // Cursor z
            provider.readBoolean(); // If the player's head is inside of a block
            WorldManager.getInstance().getContainerManager().lastInteractedWith(coords);
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

