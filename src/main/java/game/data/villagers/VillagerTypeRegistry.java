package game.data.villagers;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

public class VillagerTypeRegistry {

    private Map<Integer, String> types;

    public static VillagerTypeRegistry fromRegistry(FileInputStream input) {
        if (input == null) {
            return new VillagerTypeRegistry();
        }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        VillagerTypeRegistry villagerTypeRegistry = new VillagerTypeRegistry();
        map.get("minecraft:villager_type").getEntries()
                .forEach((name, properties) -> villagerTypeRegistry.types.put(properties.get("protocol_id"), name));

        return villagerTypeRegistry;
    }

    public VillagerTypeRegistry() {
        types = new HashMap<>();
    }

    public String getType(int id) {
        return types.getOrDefault(id, "minecraft:none");
    }

}