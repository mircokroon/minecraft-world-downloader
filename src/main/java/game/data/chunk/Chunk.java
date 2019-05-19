package game.data.chunk;

import game.Game;
import game.data.Coordinate2D;
import game.data.Dimension;
import game.data.WorldManager;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class Chunk {
    protected static final int LIGHT_SIZE = 2048;
    protected static final int CHUNK_HEIGHT = 256;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_WIDTH = 16;

    private static final int DataVersion = 1631;

    private int x;
    private int z;
    private List<SpecificTag> tileEntities;
    private ChunkSection[] chunkSections;

    private boolean saved;

    public Chunk(int x, int z) {
        this.saved = false;
        this.x = x;
        this.z = z;

        chunkSections = new ChunkSection[16];
        tileEntities = new ArrayList<>();

        WorldManager.loadChunk(new Coordinate2D(x, z), this);
    }

    private static int getInt(CompoundTag tag, String name) {
        IntTag intTag = (IntTag) tag.get(name);
        return intTag.getData();
    }

    private static String getString(CompoundTag tag, String name) {
        StringTag intTag = (StringTag) tag.get(name);
        return intTag.getData();
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public void addTileEntity(SpecificTag tag) {
        tileEntities.add(tag);
    }

    /**
     * Read a chunk column. Largely based on: https://wiki.vg/Protocol
     */
    public void readChunkColumn(boolean full, int mask, DataTypeProvider dataProvider) {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?
                readBlockCount(dataProvider);

                byte bitsPerBlock = dataProvider.readNext();

                Palette palette = Palette.readPalette(bitsPerBlock, dataProvider);

                // A bitmask that contains bitsPerBlock set bits
                int dataArrayLength = dataProvider.readVarInt();

                ChunkSection section = createNewChunkSection((byte) (sectionY & 0x0F), palette, bitsPerBlock);

                // if the chunk has no blocks
                if (dataArrayLength == 0) {
                    return;
                }
                // parse blocks
                section.setBlocks(dataProvider.readLongArray(dataArrayLength));

                parseLights(section, dataProvider);

                // dont set section if it only has air or nothing at all
                if (!palette.isEmpty()) {
                    // May replace an existing section or a null one
                    setSection(sectionY, section);
                }
            }
        }

        // biome data is only present in full chunks
        if (full) {
            readBiomes(dataProvider);
        }
    }

    protected void readBlockCount(DataTypeProvider provider) { }
    protected abstract ChunkSection createNewChunkSection(byte y, Palette palette, int bitsPerBlock);
    protected abstract void readBiomes(DataTypeProvider provider);
    protected abstract SpecificTag getBiomes();
    protected void parseLights(ChunkSection section, DataTypeProvider dataProvider) {
        section.setBlockLight(dataProvider.readByteArray(LIGHT_SIZE));

        if (Game.getDimension() == Dimension.OVERWORLD) {
            section.setSkyLight(dataProvider.readByteArray(LIGHT_SIZE));
        }
    }



    private void setSection(int sectionY, ChunkSection section) {
        chunkSections[sectionY] = section;
    }

    /**
     * Convert this chunk to NBT tags.
     * @return the nbt root tag
     */
    public NamedTag toNbt() {
        if (!hasSections()) {
            return null;
        }

        CompoundTag root = new CompoundTag();
        root.add("Level", createNbtLevel());
        root.add("DataVersion", new IntTag(Game.getDataVersion()));

        return new NamedTag("", root);
    }

    private boolean hasSections() {
        for (ChunkSection section : chunkSections) {
            if (section != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create the level tag in the NBT.
     * @return the level tag
     */
    private CompoundTag createNbtLevel() {
        CompoundTag levelTag = new CompoundTag();
        addLevelNbtTags(levelTag);
        return levelTag;
    }

    protected void addLevelNbtTags(CompoundTag map) {
        map.add("xPos", new IntTag(x));
        map.add("zPos", new IntTag(z));
        map.add("TerrainPopulated", new ByteTag((byte) 1));
        map.add("LightPopulated", new ByteTag((byte) 1));
        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>()));

        map.add("Biomes", getBiomes());
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, tileEntities));
        map.add("Sections", new ListTag(Tag.TAG_COMPOUND, getSectionList()));
    }

    /**
     * Get a list of section tags for the NBT.
     */
    private List<SpecificTag> getSectionList() {
        return Arrays.stream(chunkSections)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(ChunkSection::getY))
            .map(ChunkSection::toNbt)
            .collect(Collectors.toList());
    }

}
