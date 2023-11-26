package packets.handler;

import java.util.HashMap;
import java.util.Map;
import proxy.ConnectionManager;

public class ClientBoundConfigurationPacketHandler extends PacketHandler {
    public ClientBoundConfigurationPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return new HashMap<>();
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
