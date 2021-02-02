package packets.handler;

import game.Config;
import game.data.Coordinate3D;
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

            Coordinate3D playerPos = new Coordinate3D(x, y, z);
            playerPos.offsetGlobal();
            Config.setPlayerPosition(playerPos);

            return true;
        };

        operations.put("player_position", updatePlayerPosition);
        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("right_click", provider -> {
            // newer versions first include a VarInt with the hand
            if (Config.getProtocolVersion() >= 477) {
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

