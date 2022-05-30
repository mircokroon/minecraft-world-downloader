package game.data.chunk;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BlockEntityRegistry {

    private Map<Integer, String> blockEntities;

    public static BlockEntityRegistry fromRegistry(FileInputStream input) {
        if (input == null) { return new BlockEntityRegistry(); }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        BlockEntityRegistry blockEntityRegistry = new BlockEntityRegistry();
        map.get("minecraft:block_entity_type").getEntries().forEach(
            (name, properties) -> blockEntityRegistry.blockEntities.put(properties.get("protocol_id"), name)
        );

        return blockEntityRegistry;
    }

    public BlockEntityRegistry() {
        blockEntities = new HashMap<>();
    }

    public String getBlockEntityName(int protocolId) {
        return blockEntities.get(protocolId);
    }
}
