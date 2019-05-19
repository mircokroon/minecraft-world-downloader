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
import java.util.stream.Collectors;

/**
 * Class to handle versions from version.json file.
 */
public class VersionHandler {
    private static final String VERSION_PATH = "version.json";
    private HashMap<Integer, Protocol> protocols;

    // instantiated by Gson
    private VersionHandler() {}

    /**
     * Create a new version handler by reading from version.json file. This file contains the packet IDs for each
     * of the supported protocol versions.
     */
    public static VersionHandler createVersionHandler() {
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

        Reader file = new InputStreamReader(VersionHandler.class.getClassLoader().getResourceAsStream(VERSION_PATH));

        return g.create().fromJson(file, VersionHandler.class);
    }

    /**
     * Get a protocol object, which contains the current game and protocol versions, as well as the packet IDs
     * for the specific version. If the protocol version does not exist, it will pick the closest available one
     * that is lower than the given version. If the given version is lower than all the existing ones, pick the lowest
     * one.
     * @param protocolVersion the protocol version (not game version)
     */
    public Protocol getProtocol(int protocolVersion) {
        List<Integer> versions = protocols.keySet().stream().sorted().collect(Collectors.toList());
        int chosenVersion = versions.get(0);
        for (Integer currentVersion : versions) {
            if (protocolVersion >= currentVersion) {
                chosenVersion = currentVersion;
            }
        }
        return protocols.get(chosenVersion);
    }
}
