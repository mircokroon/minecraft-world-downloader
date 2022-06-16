package game.data.dimension;

import com.google.gson.*;
import game.data.chunk.palette.StateProvider;
import se.llbit.nbt.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage for custom dimensions, dimension types and biomes. Default biomes/dimensions are also sent by the server but
 * not stored.
 */
public class DimensionCodec {
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


    private BiomeProvider biomeProvider;

    private final Map<String, Dimension> dimensions;
    private final Map<Integer, DimensionType> dimensionTypesByHash;
    private final Map<String, DimensionType> dimensionTypesByName;
    private final Map<String, Biome> biomes;
    private DimensionCodec() {
        this.dimensions = new HashMap<>();
        this.dimensionTypesByHash = new HashMap<>();
        this.dimensionTypesByName = new HashMap<>();
        this.biomes = new HashMap<>();
    }

    public static DimensionCodec fromNbt(String[] dimensionNames, SpecificTag tag) {
        DimensionCodec codec = new DimensionCodec();

        codec.readDimensions(dimensionNames);
        codec.readDimensionTypes(tag.get("minecraft:dimension_type").asCompound().get("value").asList());
        codec.readBiomes(tag.get("minecraft:worldgen/biome").asCompound().get("value").asList());

        return codec;
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
            this.dimensionTypesByHash.put(type.getSignature(), type);
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
        this.biomeProvider = new BiomeProvider(this.biomes);
    }

    public Dimension getDimension(String name) {
        return dimensions.get(name);
    }

    /**
     * Get a dimension by it's signature. The signature is just the hash of the properties.
     */
    public DimensionType getDimensionType(int signature) {
        return dimensionTypesByHash.get(signature);
    }

    public DimensionType getDimensionType(String name) {
        return dimensionTypesByName.get(name);
    }

    /**
     * Write all the custom dimension data, if there is any.
     */
    public boolean write(Path destination) throws IOException {
        if (biomes.isEmpty() && dimensionTypesByHash.isEmpty()) {
            // nothing to write
            return false;
        }

        for (Dimension d : dimensions.values()) {
            d.write(destination);
        }

        for (DimensionType d : dimensionTypesByHash.values()) {
            d.write(destination);
        }

        for (Biome b : biomes.values()) {
            b.write(destination);
        }


        return true;
    }

    public StateProvider getBiomeProvider() {
        return biomeProvider;
    }
}
