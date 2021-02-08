package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class LoginProtocol extends Protocol {
    private String version;
    private Map<Integer, String> clientBound;
    private Map<Integer, String> serverBound;

    public LoginProtocol() {
        version = "LOGIN";
        clientBound = new HashMap<>();
        serverBound = new HashMap<>();

        clientBound.put(0x00, "disconnect");
        clientBound.put(0x01, "encryption_request");
        clientBound.put(0x02, "login_success");
        clientBound.put(0x03, "set_compression");

        serverBound.put(0x00, "login_start");
        serverBound.put(0x01, "encryption_response");
        serverBound.put(0x02, "login_plugin_response");
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
