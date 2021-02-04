package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class Protocol {
    private String version;
    private int dataVersion;
    private HashMap<Integer, String> clientBound;
    private HashMap<Integer, String> serverBound;
    private Map<String, Integer> clientBoundInverse;

    public Protocol() { }

    void generateInverse() {
        if (clientBound == null) {
            return;
        }
        clientBoundInverse = new HashMap<>();
        for (Map.Entry<Integer, String> entry : clientBound.entrySet()) {
            clientBoundInverse.put(entry.getValue(), entry.getKey());
        }
    }
    public int clientBound(String packet) {
        return clientBoundInverse.get(packet);
    }

    protected String clientBound(int packet) {
        return clientBound.getOrDefault(packet, "UNKNOWN");
    }

    protected String serverBound(int packet) {
        return serverBound.getOrDefault(packet, "UNKNOWN");
    }

    public String get(int packetID, boolean isClientBound) {
        if (isClientBound) {
            return clientBound(packetID);
        } else {
            return serverBound(packetID);
        }
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Protocol{" +
            "version='" + version + '\'' +
            ", clientbound=" + clientBound +
            ", severbound=" + serverBound +
            '}';
    }

    public int getDataVersion() {
        return dataVersion;
    }
}
