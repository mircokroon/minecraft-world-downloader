package game.data.chunk.palette;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import config.Config;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds a map of biomes and their numeric identifiers.
 */
public class BiomeRegistry implements StateProvider {
    private HashMap<String, Integer> biomes;
    private Map<Integer, BiomeIdentifier> biomesFromId;

    private BiomeRegistry() { }

    /**
     * Instantiate biome registry from json file. Ideally this should come from the server.jar reports as well, but this
     * isn't currently supported (I think).
     */
    public static BiomeRegistry create() {
        String file = "biomes-1.18.json";
        InputStream input = BiomeRegistry.class.getClassLoader().getResourceAsStream(file);

        BiomeRegistry biomeRegistry = new Gson().fromJson(new InputStreamReader(input), BiomeRegistry.class);
        biomeRegistry.init();

        return biomeRegistry;
    }

    /**
     * Create reverse of the existing map so we can find biomes by their numeric ID.
     */
    private void init() {
        biomesFromId = new HashMap<>();
        biomes.forEach((name, numericId) -> biomesFromId.put(numericId, new BiomeIdentifier(name)));
    }

    @Override
    public State getState(int i) {
        return biomesFromId.get(i);
    }

    @Override
    public State getDefaultState() {
        return biomesFromId.get(0);
    }
}

class BiomeIdentifier implements State {
    String name;

    public BiomeIdentifier(String name) {
        this.name = name;
    }

    @Override
    public SpecificTag toNbt() {
        return new StringTag("minecraft:" + name);
    }
}
