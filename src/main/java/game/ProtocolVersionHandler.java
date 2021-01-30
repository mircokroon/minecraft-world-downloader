package game;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import game.protocol.Protocol;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to handle versions from version.json file.
 */
public class ProtocolVersionHandler {
    private static final String VERSION_PATH = "version.json";
    private static ProtocolVersionHandler instance;

    private HashMap<Integer, Integer> dataVersions;

    // from JSON file
    private HashMap<Integer, Protocol> protocols;

    // instantiated by Gson
    private ProtocolVersionHandler() {}

    /**
     * Create a new version handler by reading from version.json file. This file contains the packet IDs for each
     * of the supported protocol versions.
     */
    public static ProtocolVersionHandler getInstance() {
        if (instance != null) {
            return instance;
        }

        GsonBuilder g = new GsonBuilder();
        g.registerTypeAdapter(Integer.class, new TypeAdapter() {
            @Override
            public void write(JsonWriter jsonWriter, Object o) { }

            @Override
            public Object read(JsonReader jsonReader) throws IOException {
                // use decode instead of parse so that we can use hex values to display the packet ID
                return Integer.decode(jsonReader.nextString());
            }
        });

        Reader file = new InputStreamReader(ProtocolVersionHandler.class.getClassLoader().getResourceAsStream(VERSION_PATH));

        instance = g.create().fromJson(file, ProtocolVersionHandler.class);
        instance.initialiseDataVersionMap();
        return instance;
    }

    private void initialiseDataVersionMap() {
        dataVersions = new HashMap<>();
        protocols.forEach((protocolVersion, protocol) -> {
            dataVersions.put(protocol.getDataVersion(), protocolVersion);
        });
    }

    /**
     * Get a protocol object, which contains the current game and protocol versions, as well as the packet IDs
     * for the specific version. Try to find the best matching version if the actual version is missing.
     * @param protocolVersion the protocol version (not game version)
     */
    public Protocol getProtocolByProtocolVersion(int protocolVersion) {
        return protocols.get(bestMatch(protocols.keySet(), protocolVersion));
    }

    /**
     * Get a protocol object by the data version number.
     * @param dataVersion the data version (not game version)
     */
    public Protocol getProtocolByDataVersion(int dataVersion) {
        int bestMatch = bestMatch(dataVersions.keySet(), dataVersion);

        return protocols.get(dataVersions.get(bestMatch));
    }

    /**
     * Given a set of version numbers and a target, find (in order of priority):
     *  - The version given
     *  - The closest version that's still lower than the requested version
     *  - The lowest version number
     */
    private int bestMatch(Set<Integer> values, int target) {
        if (values.contains(target)) { return target; }

        List<Integer> sorted = values.stream().sorted().collect(Collectors.toList());

        int chosenVersion = sorted.get(0);
        for (Integer currentVersion : sorted) {
            if (currentVersion < target) {
                chosenVersion = currentVersion;
            }
        }
        return chosenVersion;
    }
}
