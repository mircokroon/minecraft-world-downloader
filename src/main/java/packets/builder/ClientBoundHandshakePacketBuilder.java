package packets.builder;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundHandshakePacketBuilder extends PacketBuilder {

    @Override
    public Map<String, PacketOperator> getOperators() {
        return new HashMap<>();
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
