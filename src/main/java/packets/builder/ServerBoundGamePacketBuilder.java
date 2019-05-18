package packets.builder;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import packets.DataTypeProvider;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundGamePacketBuilder extends PacketBuilder {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundGamePacketBuilder() {
        PacketOperator updatePlayerPosition = provider -> {
            double x = provider.readDouble();
            double y = provider.readDouble();
            double z = provider.readDouble();
            Game.setPlayerPosition(new Coordinate3D(x, y, z).offset());

            return true;
        };

        operations.put("player_position", updatePlayerPosition);
        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);
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

