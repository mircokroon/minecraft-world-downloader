package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class HandshakeProtocol extends Protocol {
    private String version;
    private Map<Integer, String> clientBound;
    private Map<Integer, String> serverBound;

    public HandshakeProtocol() {
        version = "LOGIN";
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
