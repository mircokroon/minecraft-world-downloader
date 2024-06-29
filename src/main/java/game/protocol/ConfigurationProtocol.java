package game.protocol;

import config.Config;
import config.Version;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationProtocol extends Protocol {
    private final Map<Integer, String> clientBound;
    private final Map<Integer, String> serverBound;

    public ConfigurationProtocol() {
        clientBound = new HashMap<>();
        serverBound = new HashMap<>();

        if (Config.versionReporter().isAtLeast(Version.V1_20_6)) {
            serverBound.put(0x03, "FinishConfiguration");
            clientBound.put(0x07, "RegistryData");
        } else if (Config.versionReporter().isAtLeast(Version.V1_20_2)) {
            serverBound.put(0x02, "FinishConfiguration");
            clientBound.put(0x05, "RegistryData");
        } else {
            serverBound.put(0x03, "FinishConfiguration");
        }
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
