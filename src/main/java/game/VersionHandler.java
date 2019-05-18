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

public class VersionHandler {
    private static final String VERSION_PATH = "version.json";
    HashMap<Integer, Protocol> protocols;

    private VersionHandler() {}

    public static VersionHandler createVersionHandler() {
        GsonBuilder g = new GsonBuilder();
        g.registerTypeAdapter(Integer.class, new TypeAdapter() {
            @Override
            public void write(JsonWriter jsonWriter, Object o) { }

            @Override
            public Object read(JsonReader jsonReader) throws IOException {
                return Integer.decode(jsonReader.nextString());
            }
        });

        Reader file = new InputStreamReader(VersionHandler.class.getClassLoader().getResourceAsStream(VERSION_PATH));

        return g.create().fromJson(file, VersionHandler.class);
    }

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
