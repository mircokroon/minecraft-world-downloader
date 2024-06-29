package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class LoginProtocol extends Protocol {
    private final Map<Integer, String> clientBound;
    private final Map<Integer, String> serverBound;

    public LoginProtocol() {
        clientBound = new HashMap<>();
        serverBound = new HashMap<>();

        clientBound.put(0x00, "LoginDisconnect");
        clientBound.put(0x01, "Hello");
        clientBound.put(0x02, "GameProfile");
        clientBound.put(0x03, "LoginCompression");

        serverBound.put(0x00, "Hello");
        serverBound.put(0x01, "Key");
        serverBound.put(0x02, "CustomQueryAnswer");
        serverBound.put(0x03, "LoginAcknowledged");
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
