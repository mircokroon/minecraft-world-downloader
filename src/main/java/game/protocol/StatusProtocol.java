package game.protocol;

import java.util.HashMap;

public class StatusProtocol extends Protocol {
    String version;
    HashMap<Integer, String> clientbound;
    HashMap<Integer, String> severbound;

    public StatusProtocol() {
        version = "LOGIN";
        clientbound = new HashMap<>();
        severbound = new HashMap<>();
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
