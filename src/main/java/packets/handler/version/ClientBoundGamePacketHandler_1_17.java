package packets.handler.version;

import game.data.WorldManager;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;

import java.util.Map;

public class ClientBoundGamePacketHandler_1_17 extends ClientBoundGamePacketHandler_1_16 {

    public ClientBoundGamePacketHandler_1_17(ConnectionManager connectionManager) {
        super(connectionManager);

        Map<String, PacketOperator> operators = getOperators();
        operators.put("ContainerSetContent", provider -> {
            int windowId = provider.readNext();

            int stateId = provider.readVarInt();
            int count = provider.readVarInt();
            WorldManager.getInstance().getContainerManager().items(windowId, count, provider);

            return true;
        });
    }
}
