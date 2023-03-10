package game.data.chunk;

import com.google.gson.Gson;

import game.data.registries.RegistriesJson;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockEntityRegistry {

    private final Map<Integer, String> blockEntities;
    private Set<String> entityNames;

    public static BlockEntityRegistry fromRegistry(FileInputStream input) {
        if (input == null) { return new BlockEntityRegistry(); }

        RegistriesJson map = new Gson().fromJson(new InputStreamReader(input), RegistriesJson.class);

        // convert JSON structure into protocol_id->name map
        BlockEntityRegistry blockEntityRegistry = new BlockEntityRegistry();
        map.get("minecraft:block_entity_type").getEntries().forEach(
            (name, properties) -> blockEntityRegistry.blockEntities.put(properties.get("protocol_id"), name)
        );

        blockEntityRegistry.entityNames = new HashSet<>(blockEntityRegistry.blockEntities.values());

        return blockEntityRegistry;
    }

    public BlockEntityRegistry() {
        blockEntities = new HashMap<>();
        entityNames = new HashSet<>();
    }

    public String getBlockEntityName(int protocolId) {
        return blockEntities.get(protocolId);
    }

    public boolean isBlockEntity(String id) {
        return entityNames.contains(id) || isSpecialBlockEntity(id);
    }

    // should probably name this something else
    public boolean isSpecialBlockEntity(String id) {
        return id.endsWith("shulker_box")
            || id.endsWith("_bed")
            || id.endsWith("command_block")
            || id.endsWith("banner");
    }
}
