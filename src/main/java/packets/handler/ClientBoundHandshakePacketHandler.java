package packets.handler;

import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundHandshakePacketHandler extends PacketHandler {
    public ClientBoundHandshakePacketHandler(ConnectionManager connectionManager) {
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
