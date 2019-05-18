package game.protocol;

import java.util.HashMap;

public class HandshakeProtocol extends Protocol {
    String version;
    HashMap<Integer, String> clientbound;
    HashMap<Integer, String> severbound;

    public HandshakeProtocol() {
        version = "LOGIN";
        clientbound = new HashMap<>();
        severbound = new HashMap<>();

        severbound.put(0x00, "handshake");
    }

    @Override
    protected String clientBound(int packet) {
        return clientbound.getOrDefault(packet, "UNKNOWN");
    }

    @Override
    protected String serverBound(int packet) {
        return severbound.getOrDefault(packet, "UNKNOWN");
    }
}
