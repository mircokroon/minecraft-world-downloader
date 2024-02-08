package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class HandshakeProtocol extends Protocol {
    private final Map<Integer, String> clientBound;
    private final Map<Integer, String> serverBound;

    public HandshakeProtocol() {
        clientBound = new HashMap<>();
        serverBound = new HashMap<>();

        serverBound.put(0x00, "handshake");
    }

    @Override
    protected String clientBound(int packet) {
        return clientBound.getOrDefault(packet, "UNKNOWN");
    }

    @Override
    protected String serverBound(int packet) {
        return serverBound.getOrDefault(packet, "UNKNOWN");
    }
}
