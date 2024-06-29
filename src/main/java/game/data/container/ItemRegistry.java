package game.data.container;

import com.google.gson.Gson;

import game.data.chunk.palette.BlockRegistry;
import game.data.registries.RegistriesJson;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private Map<Integer, String> idToName;
    private Map<String, Integer> nameToId;

    public static ItemRegistry fromJson(String version) {
        InputStream x = BlockRegistry.class.getClassLoader().getResourceAsStream("items-" + version + ".json");
        if (x == null) {
            return null;
        }
        Gson g = new Gson();
        return g.fromJson(new InputStreamReader(x), ItemRegistry.class);
    }

    public static ItemRegistry fromRegistry(FileInputStream input) {
        if (input == null) { return new ItemRegistry(); }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        ItemRegistry itemRegistry = new ItemRegistry();
        map.get("minecraft:item").getEntries().forEach(
            (name, properties) -> {
                itemRegistry.idToName.put(properties.get("protocol_id"), name);
                itemRegistry.nameToId.put(name, properties.get("protocol_id"));
            }
        );

        return itemRegistry;
    }

    public ItemRegistry() {
        idToName = new HashMap<>();
        nameToId = new HashMap<>();
    }

    public String getItemName(int protocolId) {
        return idToName.get(protocolId);
    }
    public int getItemId(String name) {
        return nameToId.get(name);
    }
}
