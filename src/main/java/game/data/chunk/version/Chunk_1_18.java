package game.data.chunk.version;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import config.Config;
import config.Version;
import game.data.WorldManager;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.registries.RegistriesJson;
import game.data.registries.RegistryLoader;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;
import util.CompoundTagDebug;

public class Chunk_1_18 extends Chunk_1_17 {
    public static final Version VERSION = Version.V1_18;

    public Chunk_1_18(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return VERSION.dataVersion; }

    @Override
    public ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_18(y, palette, this);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_18(sectionY, section);
    }

    /**
     * Parse the chunk data.
     *
     * @param dataProvider network input
     */
    @Override
    protected void parse(DataTypeProvider dataProvider) {
        raiseEvent("parse from packet");

        parseHeightMaps(dataProvider);

        int size = dataProvider.readVarInt();
        readChunkColumn(dataProvider.ofLength(size));

        parseBlockEntities(dataProvider);

        updateLight(dataProvider);

        afterParse();
    }


    /**
     * Read a chunk column for 1.18
     */
    public void readChunkColumn(DataTypeProvider dataProvider) {
        // Loop through section Y values, starting from the lowest section that has blocks inside it.
       for (int sectionY = getMinBlockSection(); sectionY <= getMaxSection() + 1 && dataProvider.hasNext(); sectionY++) {
            ChunkSection_1_18 section = (ChunkSection_1_18) getChunkSection(sectionY);

           int blockCount = dataProvider.readShort();
           Palette blockPalette = Palette.readPalette(dataProvider);

            if (section == null) {
                section = (ChunkSection_1_18) createNewChunkSection((byte) (sectionY & 0xFF), blockPalette);
            }

            // For some reason, there's a chance that the packet has no more data. Instead of erroring,
            // just check if the packet has any more data.
            if(dataProvider.hasNext()) {
                // parse blocks
                section.setBlocks(dataProvider.readLongArray(dataProvider.readVarInt()));

                // biomes
                if(dataProvider.hasNext()) {
                    section.setBiomePalette(Palette.readPalette(dataProvider));
                    section.setBiomes(dataProvider.readLongArray(dataProvider.readVarInt()));
                }
            }

            // May replace an existing section or a null one
            setChunkSection(sectionY, section);

            // For some reason that I can't figure out, servers don't include containers
            // in the list of block_entities. We need to know that these block entities
            // exist, otherwise we'll end up not writing block entity data which results
            // in certain block entities that don't render!
            for(SpecificTag tag : blockPalette.toNbt()) {
                // Only iterate over the chunk section if the palette contains a block entity
                String blockName = tag.get("Name").stringValue();
                if(isBlockEntity(blockName)) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                final BlockState state = GlobalPaletteProvider.getGlobalPalette(getDataVersion()).getState(section.getNumericBlockStateAt(x, y, z));
                                if(isBlockEntity(state.getName())) {
                                    Coordinate3D coords = new Coordinate3D(getLocation().getX() * 16 + x, sectionY * 16 + y, getLocation().getZ() * 16 + z);
                                    this.addBlockEntity(coords, this.generateBlockEntity(state.getName(), coords));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Checks if a resource location (for a block state) is a block entity (TODO:
    // This isn't exhaustive. This should probably use a registry or something)
    public boolean isBlockEntity(String blockStateName) {
        final Set<String> blockEntities = Set.of("minecraft:chest", "minecraft:barrel", "minecraft:hopper",
                "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:blast_furnace",
                "minecraft:brewing_stand", "minecraft:furnace", "minecraft:lectern",
                "minecraft:smoker", "minecraft:conduit", "minecraft:bell");
        return blockStateName.endsWith("shulker_box") || blockStateName.endsWith("_bed")
                || blockEntities.contains(blockStateName);
    }

    @Override
    public PacketBuilder toLightPacket() {
        return null;
    }

    @Override
    protected void parseBlockEntities(DataTypeProvider dataProvider) {
        int blockEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < blockEntityCount; i++) {
            byte xz = dataProvider.readNext();
            int x = xz >> 4;
            int z = xz & 0b1111;
            int y = dataProvider.readShort();
            int type = dataProvider.readVarInt();

            // Get the exact coordinates in the world
            x = (this.getLocation().getX() * 16) + x;
            z = (this.getLocation().getZ() * 16) + z;

            SpecificTag tag = dataProvider.readNbtTag();
            if (tag instanceof CompoundTag entity) {
                String blockEntityID = WorldManager.getInstance().getBlockEntityMap().getBlockEntityName(type);

                entity.add("id", new StringTag(blockEntityID));
                addBlockEntity(new Coordinate3D(x, y, z), entity);
            }
        }
    }

    /**
     * Convert this chunk to NBT tags.
     *
     * @return the nbt root tag
     */
    public NamedTag toNbt() {
        if (!hasSections()) {
            return null;
        }

        CompoundTag root = new CompoundTag();
        if (getLocation().getX() == -7 && getLocation().getZ() == -7) {
            root = new CompoundTagDebug();
        }
        addLevelNbtTags(root);
        root.add("DataVersion", new IntTag(getDataVersion()));

        return new NamedTag("", root);
    }

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        addGeneralLevelTags(map);
        map.add("yPos", new IntTag(getMinBlockSection()));

        map.add("Heightmaps", heightMap);
        map.add("Status", new StringTag("full"));

        CompoundTag structures = new CompoundTag();
        structures.add("References", new CompoundTag());
        structures.add("Starts", new CompoundTag());
        map.add("Structures", structures);

        map.add("sections", new ListTag(Tag.TAG_COMPOUND, getSectionList()));

        addBlockEntities(map);
    }


}
