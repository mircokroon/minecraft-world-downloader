package game.data.container;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MenuRegistry {
    private static Map<String, Integer> NUM_SLOTS;
    static {
        NUM_SLOTS = new HashMap<>();
        NUM_SLOTS.put("minecraft:generic_3x3", 9);
        NUM_SLOTS.put("minecraft:generic_9x1", 9);
        NUM_SLOTS.put("minecraft:generic_9x2", 18);
        NUM_SLOTS.put("minecraft:generic_9x3", 27);
        NUM_SLOTS.put("minecraft:generic_9x4", 36);
        NUM_SLOTS.put("minecraft:generic_9x5", 45);
        NUM_SLOTS.put("minecraft:generic_9x6", 54);

        NUM_SLOTS.put("minecraft:blast_furnace", 3);
        NUM_SLOTS.put("minecraft:brewing_stand", 5);
        NUM_SLOTS.put("minecraft:furnace", 3);
        NUM_SLOTS.put("minecraft:hopper", 5);
        NUM_SLOTS.put("minecraft:shulker_box", 27);
        NUM_SLOTS.put("minecraft:smoker", 3);
        NUM_SLOTS.put("minecraft:lectern", 1);
    }

    private Map<Integer, String> menus;

    public static MenuRegistry fromRegistry(FileInputStream input) {
        if (input == null) { return new MenuRegistry(); }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        MenuRegistry menuRegistry = new MenuRegistry();
        map.get("minecraft:menu").getEntries().forEach(
            (name, properties) -> menuRegistry.menus.put(properties.get("protocol_id"), name)
        );

        return menuRegistry;
    }

    public MenuRegistry() {
        menus = new HashMap<>();
    }

    public int getSlotCount(int protocolId) {
        String inventoryName = getName(protocolId);
        return NUM_SLOTS.getOrDefault(inventoryName, 0);
    }

    public String getName(int protocolId) {
        return menus.getOrDefault(protocolId, "");
    }
}
