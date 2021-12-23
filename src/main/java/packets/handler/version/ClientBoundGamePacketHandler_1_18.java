package packets.handler.version;

import game.data.WorldManager;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;

import java.util.Map;

public class ClientBoundGamePacketHandler_1_18 extends ClientBoundGamePacketHandler_1_17 {
    public ClientBoundGamePacketHandler_1_18(ConnectionManager connectionManager) {
        super(connectionManager);

        WorldManager worldManager = WorldManager.getInstance();
        Map<String, PacketOperator> operators = getOperators();
        operators.put("LevelChunkWithLight", provider -> {
            try {
                worldManager.getChunkFactory().addChunk(provider);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        });
    }

}
