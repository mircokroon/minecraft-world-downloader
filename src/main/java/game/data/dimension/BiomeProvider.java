package game.data.dimension;

import game.data.chunk.palette.State;
import game.data.chunk.palette.StateProvider;
import java.util.HashMap;
import java.util.Map;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

public class BiomeProvider implements StateProvider {
    private final Map<Integer, BiomeIdentifier> biomesFromId;

    public BiomeProvider(Map<String, Biome> biomes) {
        biomesFromId = new HashMap<>();

        biomes.forEach((name, biome) -> {
            biomesFromId.put(biome.id, new BiomeIdentifier(name));
        });
    }

    @Override
    public State getState(int i) {
        return biomesFromId.get(i);
    }

    @Override
    public State getDefaultState() {
        return biomesFromId.get(0);
    }
}

class BiomeIdentifier implements State {
    String name;

    public BiomeIdentifier(String name) {
        this.name = name;
    }

    @Override
    public SpecificTag toNbt() {
        return new StringTag(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
