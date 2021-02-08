package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class StatusProtocol extends Protocol {
    private String version;
    private Map<Integer, String> clientBound;
    private Map<Integer, String> serverBound;

    public StatusProtocol() {
        version = "LOGIN";
        clientBound = new HashMap<>();
        serverBound = new HashMap<>();
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
