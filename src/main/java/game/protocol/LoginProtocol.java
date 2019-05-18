package game.protocol;

import java.util.HashMap;

public class LoginProtocol extends Protocol {
    String version;
    HashMap<Integer, String> clientbound;
    HashMap<Integer, String> severbound;

    public LoginProtocol() {
        version = "LOGIN";
        clientbound = new HashMap<>();
        severbound = new HashMap<>();

        clientbound.put(0x00, "disconnect");
        clientbound.put(0x01, "encryption_request");
        clientbound.put(0x02, "login_success");
        clientbound.put(0x03, "set_compression");

        severbound.put(0x00, "login_start");
        severbound.put(0x01, "encryption_response");
        severbound.put(0x02, "login_plugin_response");
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
