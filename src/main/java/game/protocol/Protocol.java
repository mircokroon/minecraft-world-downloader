package game.protocol;

import java.util.HashMap;

public class Protocol {
    String version;
    int dataVersion;
    HashMap<Integer, String> clientbound;
    HashMap<Integer, String> serverbound;

    public Protocol() {}

    protected String clientBound(int packet) {
        return clientbound.getOrDefault(packet, "UNKNOWN");
    }

    protected String serverBound(int packet) {
        return serverbound.getOrDefault(packet, "UNKNOWN");
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
            ", clientbound=" + clientbound +
            ", severbound=" + serverbound +
            '}';
    }

    public int getDataVersion() {
        return dataVersion;
    }
}
