package packets.handler;

import config.Config;
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

        operations.put("player_position", updatePlayerPosition);
        operations.put("player_look", updatePlayerRotation);
        operations.put("player_position_look", (provider) -> {
            updatePlayerPosition.apply(provider);
            updatePlayerRotation.apply(provider);
            return true;
        });
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("right_click", provider -> {
            // newer versions first include a VarInt with the hand
            if (Config.versionReporter().isAtLeast1_14()) {
                provider.readVarInt();
            }

            WorldManager.getInstance().getContainerManager().lastInteractedWith(provider.readCoordinates());

            return true;
        });
        operations.put("close_window", provider -> {
            WorldManager.getInstance().getContainerManager().closeWindow(provider.readNext());
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

