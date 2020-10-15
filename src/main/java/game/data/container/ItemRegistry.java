package game.data.container;

import com.google.gson.Gson;

import game.data.chunk.palette.GlobalPalette;
import game.data.registries.RegistriesJson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private Map<Integer, String> items;

    public static ItemRegistry fromJson(String version) {
        InputStream x = GlobalPalette.class.getClassLoader().getResourceAsStream("items-" + version + ".json");
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
            (name, properties) -> itemRegistry.items.put(properties.get("protocol_id"), name)
        );

        return itemRegistry;
    }

    public ItemRegistry() {
        items = new HashMap<>();
    }

    public String getItemName(int protocolId) {
        return items.get(protocolId);
    }
}
