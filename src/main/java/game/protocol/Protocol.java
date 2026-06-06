package game.protocol;

import java.util.HashMap;
import java.util.Map;

public class Protocol {
    private String version;
    private int dataVersion;
    private HashMap<Integer, String> clientBound;
    private HashMap<Integer, String> serverBound;
    private Map<String, Integer> clientBoundInverse;
    private Map<String, Integer> serverBoundInverse;

    public Protocol() { }

    void generateInverse() {
        if (clientBound != null) {
            clientBoundInverse = new HashMap<>();
            for (Map.Entry<Integer, String> entry : clientBound.entrySet()) {
                clientBoundInverse.put(entry.getValue(), entry.getKey());
            }
        }
        if (serverBound != null) {
            serverBoundInverse = new HashMap<>();
            for (Map.Entry<Integer, String> entry : serverBound.entrySet()) {
                serverBoundInverse.put(entry.getValue(), entry.getKey());
            }
        }
    }
    public int clientBound(String packet) {
        return clientBoundInverse.get(packet);
    }

    /** Forward lookup (name -> id) for serverbound packets; -1 if unknown for this version. */
    public int serverBound(String packet) {
        if (serverBoundInverse == null) {
            return -1;
        }
        return serverBoundInverse.getOrDefault(packet, -1);
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
