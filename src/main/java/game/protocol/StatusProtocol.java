package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class StatusProtocol extends Protocol {
    private final Map<Integer, String> clientBound;
    private final Map<Integer, String> serverBound;

    public StatusProtocol() {
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
