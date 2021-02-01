package packets.handler;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundHandshakePacketHandler extends PacketHandler {

    @Override
    public Map<String, PacketOperator> getOperators() {
        return new HashMap<>();
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
