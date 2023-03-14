package game.data.dimension;

import se.llbit.nbt.*;
import util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds biomes that are registered the server. The server does not give us any information about world generation in
 * these biomes so the client can't generate more of them, but we still need to register them as they may be used
 * in the world.
 */
public class Biome {
    String namespace;
    String path;
    String name;
    int id;
    BiomeProperties properties;

    public Biome(int id) {
        this.id = id;
    }

    public Biome(String namespace, String fullName, int id, CompoundTag properties) {
        this.namespace = namespace;
        this.id = id;

        // if a name is of the structure "dimension/biome" then we need to use the dimension as path
        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash > 0) {
            this.path = fullName.substring(0, lastSlash);
            this.name = fullName.substring(lastSlash + 1);
        } else {
            this.path = "";
            this.name = fullName;
        }
        this.properties = new BiomeProperties(properties);
    }

    /**
     * Write this biome to the world/biome folder.
     */
    public void write(Path fromPath) throws IOException {
        Path p = PathUtils.toPath(fromPath.toString(), namespace, "worldgen", "biome", path, name + ".json");
        Files.createDirectories(p.getParent());
        Files.write(p, Collections.singleton(properties.json()));
    }
}

/**
 * The biome properties have some fields that we should make sure to always have, so we use some defaults in case the
 * server does not send them all.
 */
class BiomeProperties {
    // these are the minimal properties we need, some of which are not sent by the server
    static Map<String, SpecificTag> defaultProperties;
    static {
        defaultProperties = new HashMap<>();
        defaultProperties.put("surface_builder", new StringTag("minecraft:nope"));
        defaultProperties.put("precipitation", new StringTag("rain"));
        defaultProperties.put("category", new StringTag("none"));

        defaultProperties.put("depth", new FloatTag(0));
        defaultProperties.put("scale", new FloatTag(1));
        defaultProperties.put("temperature", new FloatTag(0));
        defaultProperties.put("downfall", new FloatTag(0));

        defaultProperties.put("starts", new ListTag(Tag.TAG_BYTE, Collections.emptyList()));
        defaultProperties.put("features", new ListTag(Tag.TAG_BYTE, Collections.emptyList()));
        defaultProperties.put("spawners", new CompoundTag());
        defaultProperties.put("spawn_costs", new CompoundTag());
        defaultProperties.put("carvers", new CompoundTag());
    }

    Map<String, SpecificTag> properties;

    BiomeProperties(CompoundTag tag) {
        properties = new HashMap<>(defaultProperties);

        for (NamedTag el : tag) {
            properties.put(el.name, el.tag);
        }
    }

    /**
     * Datapack stores this as a JSON file.
     */
    public String json() {
        return DimensionCodec.GSON.toJson(this.properties);
    }
}
