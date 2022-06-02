package game.data.villagers;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

public class VillagerProfessionRegistry {

    private Map<Integer, String> professions;

    public static VillagerProfessionRegistry fromRegistry(FileInputStream input) {
        if (input == null) {
            return new VillagerProfessionRegistry();
        }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        VillagerProfessionRegistry professionRegistry = new VillagerProfessionRegistry();
        map.get("minecraft:villager_profession").getEntries()
                .forEach((name, properties) -> professionRegistry.professions.put(properties.get("protocol_id"), name));

        return professionRegistry;
    }

    public VillagerProfessionRegistry() {
        professions = new HashMap<>();
    }

    public String getProfession(int id) {
        return professions.getOrDefault(id, "minecraft:none");
    }

}