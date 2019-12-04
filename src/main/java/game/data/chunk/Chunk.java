package game.data.chunk;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.Dimension;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import gui.GuiManager;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.Tag;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Basic chunk class. May be extended by version-specific ones as they can have implementation differences.
 */
public abstract class Chunk {
    protected static final int LIGHT_SIZE = 2048;
    protected static final int CHUNK_HEIGHT = 256;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_WIDTH = 16;

    public final int x;
    public final int z;
    private Map<Coordinate3D, SpecificTag> tileEntities;
    private ChunkSection[] chunkSections;

    private Runnable afterParse;
    private boolean isNewChunk;

    private boolean saved;

    public Chunk(int x, int z) {
        this.saved = false;
        this.x = x;
        this.z = z;
        this.isNewChunk = false;

        chunkSections = new ChunkSection[16];
        tileEntities = new HashMap<>();

        WorldManager.loadChunk(new Coordinate2D(x, z), this);
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /**
     * Allows a callback to be called when the chunk is done being parsed.
     */
    public void whenParsed(Runnable r) {
        afterParse = r;
    }

    public void addTileEntity(Coordinate3D location, SpecificTag tag) {
        CompoundTag entity = (CompoundTag) tag;

        // remove existing coordinates
        Iterator<NamedTag> it = entity.iterator();
        while (it.hasNext()) {
            NamedTag t = it.next();
            if (t.isNamed("x") || t.isNamed("y") || t.isNamed("z")) { it.remove(); }
        }

        // insert new coordinates (offset)
        entity.add("x", new IntTag(location.getX()));
        entity.add("y", new IntTag(location.getY()));
        entity.add("z", new IntTag(location.getZ()));

        tileEntities.put(location, tag);
    }

    /**
     * Add a tile entity, if the position is not known yet. This method will also offset the position.
     * @param nbtTag the NBT data of the tile entity, should include X, Y, Z of the entity
     */
    private void addTileEntity(SpecificTag nbtTag) {
        CompoundTag entity = (CompoundTag) nbtTag;
        Coordinate3D position = new Coordinate3D(entity.get("x").intValue(), entity.get("y").intValue(), entity.get("z").intValue());
        position.offsetGlobal();

        addTileEntity(position, nbtTag);
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
    protected void parseHeightMaps(DataTypeProvider dataProvider) { }
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

    /**
     * Add NBT tags to the level tag. May be overriden by versioned chunks to add extra tags. Those should probably
     * call this (super) method.
     */
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("xPos", new IntTag(x));
        map.add("zPos", new IntTag(z));
        map.add("TerrainPopulated", new ByteTag((byte) 1));
        map.add("LightPopulated", new ByteTag((byte) 1));
        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>()));

        map.add("Biomes", getBiomes());
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(tileEntities.values())));
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

    /**
     * Parse the chunk data.
     * @param dataProvider network input
     * @param full indicates if its the full chunk or a part of it
     */
    void parse(DataTypeProvider dataProvider, boolean full) {
        int mask = dataProvider.readVarInt();

        // for 1.14+
        parseHeightMaps(dataProvider);

        int size = dataProvider.readVarInt();

        readChunkColumn(full, mask, dataProvider);

        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            addTileEntity(dataProvider.readNbtTag());
        }

        // ensure the chunk is (re)saved
        this.saved = false;

        // run the callback if one exists
        if (afterParse != null) {
            afterParse.run();
        }
    }

    public abstract Image getImage();

    public BlockState topmostBlockStateAt(int x, int z) {
        for (int chunkSection = 15; chunkSection >= 0; chunkSection--) {
            if (chunkSections[chunkSection] == null) { continue; }

            ChunkSection s = chunkSections[chunkSection];
            BlockState block = s.topmostBlockStateAt(x, z);
            if (block == null) { continue; }

            return block;
        }
        return null;
    }

    /**
     * Mark this as a new chunk iff the
     */
    void markAsNew() {
        if (WorldManager.markNewChunks()) {
            this.isNewChunk = true;
        }
    }

    protected boolean isNewChunk() {
        return isNewChunk;
    }
}
