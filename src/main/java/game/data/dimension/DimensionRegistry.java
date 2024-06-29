package game.data.dimension;

import com.google.gson.*;
import config.Config;
import game.data.chunk.palette.Registry;
import java.util.List;
import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage for custom dimensions, dimension types and biomes. Default biomes/dimensions are also sent by the server but
 * not stored.
 */
public class DimensionRegistry {
    public static final Gson GSON;

    static {
        /*
         * To convert the properties to JSON, we need to register adapters so that GSON knows how to turn our NBT object
         * into desirable JSON.
         */
        GsonBuilder g = new GsonBuilder();
        g.registerTypeAdapter(StringTag.class, (JsonSerializer<StringTag>) (str, x, y) -> new JsonPrimitive(str.value));
        g.registerTypeAdapter(DoubleTag.class, (JsonSerializer<DoubleTag>) (num, x, y) -> new JsonPrimitive(num.value));
        g.registerTypeAdapter(FloatTag.class, (JsonSerializer<FloatTag>) (num, x, y) -> new JsonPrimitive(num.value));
        g.registerTypeAdapter(IntTag.class, (JsonSerializer<IntTag>) (num, x, y) -> new JsonPrimitive(num.value));
        g.registerTypeAdapter(NamedTag.class, (JsonSerializer<NamedTag>) (tag, x, ctx) -> ctx.serialize(tag.tag));
        g.registerTypeAdapter(ByteTag.class, (JsonSerializer<ByteTag>) (tag, x, ctx) -> ctx.serialize(tag.value == 1));
        g.registerTypeAdapter(CompoundTag.class, (JsonSerializer<CompoundTag>) (list, x, ctx) -> {
            JsonObject obj = new JsonObject();

            for (NamedTag t : list) {
                obj.add(t.name, ctx.serialize(t));
            }

            return obj;
        });
        g.registerTypeAdapter(ListTag.class, (JsonSerializer<ListTag>) (list, x, ctx) -> {
            JsonArray arr = new JsonArray();

            for (SpecificTag t : list) {
                arr.add(ctx.serialize(t));
            }

            return arr;
        });
        GSON = g.create();
    }


    private BiomeRegistry biomeRegistry;

    private final Map<String, Dimension> dimensions;
    private final Map<Integer, DimensionType> dimensionTypesByID;
    private final Map<String, DimensionType> dimensionTypesByName;
    private final Map<String, Biome> biomes;

    private DimensionRegistry() {
        this.dimensions = new HashMap<>();
        this.dimensionTypesByName = new HashMap<>();
        this.dimensionTypesByID = new HashMap<>();
        this.biomes = new HashMap<>();

        this.dimensions.put("minecraft:overworld", Dimension.OVERWORLD);
        this.dimensions.put("minecraft:the_nether", Dimension.NETHER);
        this.dimensions.put("minecraft:the_end", Dimension.END);
    }

    public static DimensionRegistry empty() {
        return new DimensionRegistry();
    }

    public static DimensionRegistry fromNbt(SpecificTag tag) {
        DimensionRegistry codec = new DimensionRegistry();

        codec.readDimensionTypes(tag.get("minecraft:dimension_type").asCompound().get("value").asList());
        codec.readBiomes(tag.get("minecraft:worldgen/biome").asCompound().get("value").asList());

        return codec;
    }

    public void setDimensionNames(String[] dimensionNames) {
        this.readDimensions(dimensionNames);
    }

    public Collection<Dimension> getDimensions() {
        return dimensions.values();
    }

    private void readDimensions(String[] dimensionNames) {
        for (String dimensionName : dimensionNames) {
            String[] parts = dimensionName.split(":");
            String namespace = parts[0];
            String name = parts[1];

            this.dimensions.put(dimensionName, new Dimension(namespace, name));
        }
    }

    /**
     * Dimension types. These are not very useful currently as the server does not actually let the client know which
     * dimensions have which types.
     */
    private void readDimensionTypes(ListTag dimensionList) {
        for (SpecificTag dim : dimensionList) {
            CompoundTag d = dim.asCompound();

            String identifier = ((StringTag) d.get("name")).value;
            String[] parts = identifier.split(":");
            String namespace = parts[0];
            String name = parts[1];

            DimensionType type = new DimensionType(namespace, name, d);
            this.dimensionTypesByID.put(type.getSignature(), type);
            this.dimensionTypesByName.put(type.getName(), type);
        }
    }

    /**
     * Biome registry.
     */
    private void readBiomes(ListTag biomeList) {
        for (SpecificTag biome : biomeList) {
            CompoundTag b = biome.asCompound();

            String identifier = ((StringTag) b.get("name")).value;
            int id = ((IntTag) b.get("id")).value;
            String[] parts = identifier.split(":");
            String namespace = parts[0];
            String name = parts[1];

            this.biomes.put(identifier, new Biome(namespace, name, id, b.get("element").asCompound()));
        }
        this.biomeRegistry = new BiomeRegistry(this.biomes);
    }

    public Dimension getDimension(String name) {
        var dim = dimensions.get(name);

        if (dim == null) {
            System.out.println("Warning: Dimension " + name + " not found, using overworld");
            return Dimension.OVERWORLD;
        }

        return dim;
    }

    /**
     * Get a dimension by it's ID. Pre-1.19 the ID is the hash of the properties.
     */
    public DimensionType getDimensionType(int id) {
        return dimensionTypesByID.get(id);
    }

    public DimensionType getDimensionType(String id) {
        return dimensionTypesByName.get(id);
    }

    /**
     * Write all the custom dimension data, if there is any.
     */
    public boolean write(Path destination) throws IOException {
        if (biomes.isEmpty() && dimensionTypesByID.isEmpty()) {
            // nothing to write
            return false;
        }

        for (Dimension d : dimensions.values()) {
            d.write(destination);
        }

        for (DimensionType d : dimensionTypesByID.values()) {
            d.write(destination);
        }

        for (Biome b : biomes.values()) {
            b.write(destination);
        }


        return true;
    }

    public Registry getBiomeRegistry() {
        return biomeRegistry;
    }

    public void loadBiomes(DataTypeProvider.Registry registry) {
        this.biomeRegistry = new BiomeRegistry();

        List<DataTypeProvider.RegistryEntry> entries = registry.entries();
        for (int id = 0; id < entries.size(); id++) {
            var biome = entries.get(id);

            String[] parts = biome.name().split(":");
            String namespace = parts[0];
            String name = parts[1];

            var res = new Biome(namespace, name, id, biome.nbt().map(Tag::asCompound).orElse(null));
            this.biomes.put(biome.name(), res);
            this.biomeRegistry.addBiome(biome.name(), res);
        }
    }

    public void loadDimensions(DataTypeProvider.Registry registry) {
        List<DataTypeProvider.RegistryEntry> entries = registry.entries();
        for (int id = 0; id < entries.size(); id++) {
            var dim = entries.get(id);

            String[] parts = dim.name().split(":");
            String namespace = parts[0];
            String name = parts[1];

            DimensionType type = new DimensionType(namespace, name, id, dim.nbt().map(Tag::asCompound).orElse(null));
            this.dimensionTypesByID.put(id, type);
            this.dimensionTypesByName.put(type.getName(), type);
        }
    }
}
