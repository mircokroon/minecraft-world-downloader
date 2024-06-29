package game.data.registries.modded;

import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.palette.GlobalPaletteProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import packets.DataTypeProvider;
import packets.handler.PacketOperator;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

/**
 * Handle plugin channel messages for Forge (at least on 1.12.2)
 */
public class ForgeRegistryHandler implements PacketOperator {
    private static final byte TYPE_MODLIST = 0x02;
    private static final byte TYPE_REGISTRY = 0x03;

    private final Map<String, ForgeRegistry> registries;
    private final List<Pair<String, String>> modList;

    public ForgeRegistryHandler() {
        this.registries = new HashMap<>();
        this.modList = new ArrayList<>();

        // add ForgeDataVersion to chunks, not sure if this does anything
        Chunk.registerNbtModifier((chunk, tag) -> {
            CompoundTag forgeData = new CompoundTag();
            forgeData.add("DataVersion", new IntTag(chunk.getDataVersion()));

            tag.asCompound().add("ForgeDataVersion", forgeData);
        });

        // include mod list and registries in the level.dat file
        WorldManager.getInstance().registerLevelDataModifier(tag -> {
            CompoundTag root = tag.asCompound();

            CompoundTag fml = new CompoundTag();
            fml.add("Registries", new CompoundTag(
                registries.entrySet().stream()
                    .map((e) -> new NamedTag(e.getKey(), e.getValue().toNbt().asCompound()))
                    .toList()
            ));
            fml.add("ModList", new ListTag(Tag.TAG_COMPOUND, modList.stream().map(pair -> new CompoundTag(List.of(
                new NamedTag("ModId", new StringTag(pair.getKey())),
                new NamedTag("ModVersion", new StringTag(pair.getValue()))))).toList()));
            root.add("FML", fml);
        });
    }

    @Override
    public Boolean apply(DataTypeProvider provider) {
        byte type = provider.readNext();

        switch (type) {
            case TYPE_MODLIST -> handleModList(provider);
            case TYPE_REGISTRY -> handleRegistry(provider);
        }

        return true;
    }

    private void handleRegistry(DataTypeProvider provider) {
        // true if another registry will follow, not sure what the point of this is
        provider.readBoolean();

        String name = provider.readString();

        ForgeRegistry registry = registries.computeIfAbsent(name, (n) -> new ForgeRegistry());

        int numIds = provider.readVarInt();
        for (int i = 0; i < numIds; i++) {
            registry.addId(provider.readString(), provider.readVarInt());
        }

        if (name.equals("minecraft:blocks")) {
            registry.registerBlocks();
        }

        int numAliases = provider.readVarInt();
        for (int i = 0; i < numAliases; i++) {
            registry.addAlias(provider.readString());
        }

        if (!provider.hasNext()) {
            return;
        }

        int numDummied = provider.readVarInt();
        for (int i = 0; i < numDummied; i++) {
            registry.addDummied(provider.readString());
        }
    }

    private void handleModList(DataTypeProvider provider) {
        int numMods = provider.readVarInt();
        for (int i = 0; i < numMods; i++) {
            modList.add(new Pair<>(provider.readString(), provider.readString()));
        }
    }
}

class ForgeRegistry {
    List<String> aliases;
    List<String> dummied;
    List<Pair<String, Integer>> ids;

    public ForgeRegistry() {
        this.aliases = new ArrayList<>();
        this.dummied = new ArrayList<>();
        this.ids = new ArrayList<>();
    }

    public void addId(String name, int id) {
        ids.add(new Pair<>(name, id));
    }

    public void addDummied(String name) {
        dummied.add(name);
    }

    public void addAlias(String name) {
        aliases.add(name);
    }

    public Tag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.add("aliases", new ListTag(Tag.TAG_STRING, aliases.stream().map(StringTag::new).toList()));
        tag.add("dummied", new ListTag(Tag.TAG_STRING, dummied.stream().map(StringTag::new).toList()));
        tag.add("ids", new ListTag(Tag.TAG_COMPOUND, ids.stream().map(pair -> {
            CompoundTag id = new CompoundTag();
            id.add("K", new StringTag(pair.getKey()));
            id.add("V", new IntTag(pair.getValue()));
            return id;
        }).toList()));

        return tag;
    }

    /**
     * Add blocks to the block registry
     */
    public void registerBlocks() {
        // since blockstates in 1.12.2 have half a byte of data at the end, we need to shift the
        // blockstates we register to the block registry for the minimap to remain correct(ish)
        ids.forEach(pair -> GlobalPaletteProvider.registerBlock(pair.getKey(), pair.getValue() << 4));
    }
}
