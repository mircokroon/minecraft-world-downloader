package game.data.dimension;

import game.data.chunk.palette.State;
import game.data.chunk.palette.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

public class BiomeRegistry implements Registry {
    private final Map<Integer, BiomeIdentifier> biomesFromId;
    private final Map<BiomeIdentifier, Integer> biomeIdsFromIdentifier;

    public BiomeRegistry(Map<String, Biome> biomes) {
        this();

        biomes.forEach(this::addBiome);
    }

    public void addBiome(String name, Biome biome) {
        biomesFromId.put(biome.id, new BiomeIdentifier(name));
        biomeIdsFromIdentifier.put(new BiomeIdentifier(name), biome.id);
    }

    public BiomeRegistry() {
       this.biomesFromId = new HashMap<>();
       this.biomeIdsFromIdentifier = new HashMap<>();
    }

    @Override
    public State getState(int i) {
        return biomesFromId.get(i);
    }

    @Override
    public int getStateId(SpecificTag nbt) {
        return biomeIdsFromIdentifier.get(new BiomeIdentifier(nbt));
    }

    @Override
    public State getState(SpecificTag nbt) {
        return new BiomeIdentifier(nbt);
    }

    @Override
    public State getDefaultState() {
        return biomesFromId.get(0);
    }

    @Override
    public String toString() {
        return "BiomeRegistry{" + biomesFromId + '}';
    }
}

class BiomeIdentifier implements State {
    String name;

    public BiomeIdentifier(String name) {
        this.name = name;
    }

    public BiomeIdentifier(SpecificTag nbt) {
        this.name = nbt.stringValue();
    }

    @Override
    public SpecificTag toNbt() {
        return new StringTag(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        BiomeIdentifier that = (BiomeIdentifier) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
