package game.data.container;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private Map<Integer, String> items;

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
