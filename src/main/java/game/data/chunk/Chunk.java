package game.data.chunk;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.protocol.Protocol;
import gui.ChunkState;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Basic chunk class. May be extended by version-specific ones as they can have implementation differences.
 */
public abstract class Chunk extends ChunkEntities {
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_WIDTH = 16;
    protected static final int LIGHT_SIZE = 2048;
    protected static final int CHUNK_HEIGHT = 256;
    private final ChunkSection[] chunkSections;
    public CoordinateDim2D location;
    private Runnable afterParse;
    private Runnable onUnload;
    private boolean isNewChunk;
    private boolean saved;
    private ChunkImageFactory imageFactory;
    private boolean isLit;

    public Chunk(CoordinateDim2D location) {
        super();

        this.isLit = false;
        this.saved = false;
        this.location = location;
        this.isNewChunk = false;

        chunkSections = new ChunkSection[16];
    }

    protected ChunkSection[] getChunkSections() {
        return chunkSections;
    }

    public abstract int getDataVersion();

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public void setOnUnload(Runnable r) {
        this.onUnload = r;
    }

    @Override
    public Dimension getDimension() {
        return location.getDimension();
    }

    /**
     * Allows a callback to be called when the chunk is done being parsed.
     */
    public void whenParsed(Runnable r) {
        if (isSaved()) {
            r.run();
        } else {
            afterParse = r;
        }
    }


    /**
     * Read a chunk column. Largely based on: https://wiki.vg/Protocol
     */
    public void readChunkColumn(boolean full, int mask, DataTypeProvider dataProvider) {
        // We shift the mask left each iteration and check the unit bit. If the mask is 0, there will be no more chunks
        // so can stop the loop early.
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT) && mask != 0; sectionY++, mask >>>= 1) {
            // Mask tells us if a section is present or not
            if ((mask & 1) == 0) {
                continue;
            }

            readBlockCount(dataProvider);

            byte bitsPerBlock = dataProvider.readNext();
            Palette palette = Palette.readPalette(bitsPerBlock, dataProvider);

            // A bitmask that contains bitsPerBlock set bits
            int dataArrayLength = dataProvider.readVarInt();

            ChunkSection section = createNewChunkSection((byte) (sectionY & 0x0F), palette);

            // if the section has no blocks
            if (dataArrayLength == 0) {
                continue;
            }
            // parse blocks
            section.setBlocks(dataProvider.readLongArray(dataArrayLength));

            parseLights(section, dataProvider);

            // don't set section if it only has air or nothing at all
            if (!palette.isEmpty()) {
                // May replace an existing section or a null one
                setSection(sectionY, section);
            }
        }

        // biome data is only present in full chunks, for <= 1.14.4
        if (full) {
            parse2DBiomeData(dataProvider);
        }
    }

    protected void parseHeightMaps(DataTypeProvider dataProvider) {
    }

    protected void readBlockCount(DataTypeProvider provider) {
    }

    protected abstract ChunkSection createNewChunkSection(byte y, Palette palette);

    protected abstract SpecificTag getNbtBiomes();

    protected void parse2DBiomeData(DataTypeProvider provider) {
    }

    protected void parse3DBiomeData(DataTypeProvider provider) {
    }

    protected void parseLights(ChunkSection section, DataTypeProvider dataProvider) {
        section.setBlockLight(dataProvider.readByteArray(LIGHT_SIZE));

        if (WorldManager.getInstance().getDimension() != Dimension.NETHER) {
            section.setSkyLight(dataProvider.readByteArray(LIGHT_SIZE));
        }
    }

    private void setSection(int sectionY, ChunkSection section) {
        chunkSections[sectionY] = section;
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
        root.add("Level", createNbtLevel());
        root.add("DataVersion", new IntTag(getDataVersion()));

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
     *
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
        super.addLevelNbtTags(map);

        Coordinate2D offset = this.location.offsetChunk();
        map.add("xPos", new IntTag(offset.getX()));
        map.add("zPos", new IntTag(offset.getZ()));

        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));

        map.add("Biomes", getNbtBiomes());
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
     *
     * @param dataProvider network input
     * @param full         indicates if its the full chunk or a part of it
     */
    void parse(DataTypeProvider dataProvider, boolean full) {
        int mask = dataProvider.readVarInt();

        // for 1.14+
        parseHeightMaps(dataProvider);

        if (full) {
            parse3DBiomeData(dataProvider);
        }

        int size = dataProvider.readVarInt();
        readChunkColumn(full, mask, dataProvider.ofLength(size));

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


    public int getNumericBlockStateAt(int x, int y, int z) {
        int section = y / SECTION_HEIGHT;
        if (chunkSections[section] == null) {
            return 0;
        }

        return chunkSections[section].getNumericBlockStateAt(x, y % SECTION_HEIGHT, z);
    }

    public BlockState getBlockStateAt(Coordinate3D location) {
        return getBlockStateAt(location.getX(), location.getY(), location.getZ());
    }

    public BlockState getBlockStateAt(int x, int y, int z) {
        int id = getNumericBlockStateAt(x, y, z);
        if (id == 0) {
            return null;
        }

        return GlobalPaletteProvider.getGlobalPalette(getDataVersion()).getState(id);
    }

    /**
     * Generate network packet for this chunk.
     */
    public PacketBuilder toPacket() {
        Protocol p = Config.versionReporter().getProtocol();
        PacketBuilder packet = new PacketBuilder();
        packet.writeVarInt(p.clientBound("chunk_data"));

        packet.writeInt(location.getX());
        packet.writeInt(location.getZ());
        packet.writeBoolean(true);

        writeBitMask(packet);
        writeHeightMaps(packet);
        writeBiomes(packet);

        // sections
        PacketBuilder columns = writeSectionData();
        byte[] columnArr = columns.toArray();
        packet.writeVarInt(columnArr.length);
        packet.writeByteArray(columnArr);

        // we don't include block entities - these chunks will be far away so they shouldn't be rendered anyway
        packet.writeVarInt(0);
        return packet;
    }

    public PacketBuilder toLightPacket() { return null; }

    protected void writeHeightMaps(PacketBuilder packet) {
    }

    protected PacketBuilder writeSectionData() {
        PacketBuilder column = new PacketBuilder();
        for (int y = 0; y < (CHUNK_HEIGHT / SECTION_HEIGHT); y++) {
            if (chunkSections[y] != null) {
                chunkSections[y].write(column);
            }
        }

        return column;
    }

    private void writeBitMask(PacketBuilder packet) {
        int res = 0;
        for (int i = 0; i < chunkSections.length; i++) {
            if (chunkSections[i] != null) {
                res |= 1 << i;
            }
        }
        packet.writeVarInt(res);
    }

    protected void writeBiomes(PacketBuilder packet) { }

    /**
     * Mark this as a new chunk if it's sent in parts, which non-vanilla servers will do to send chunks to the client
     * before they are fully generated.
     */
    void markAsNew() {
        if (WorldManager.getInstance().markNewChunks()) {
            this.isNewChunk = true;
        }
    }

    protected boolean isNewChunk() {
        return isNewChunk;
    }


    public void parse(Tag tag) {
        tag.get("Level").asCompound().get("Sections").asList().forEach(section -> {
            int sectionY = section.get("Y").byteValue();
            if (sectionY >= 0 && sectionY < this.chunkSections.length) {
                this.chunkSections[sectionY] = parseSection(sectionY, section);
            }
        });
        parseHeightMaps(tag);
        parseBiomes(tag);
    }

    protected void parseHeightMaps(Tag tag) {
    }

    protected void parseBiomes(Tag tag) {
    }

    protected abstract ChunkSection parseSection(int sectionY, SpecificTag section);

    /**
     * Mark this chunk as unsaved.
     */
    public void touch() {
        this.setSaved(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chunk chunk = (Chunk) o;

        if (!Objects.equals(location, chunk.location)) return false;
        if (!Arrays.deepEquals(chunkSections, chunk.chunkSections)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(chunkSections);
        return result;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "location=" + location +
                ", chunkSections=" + Arrays.toString(chunkSections) +
                '}';
    }

    public void unload() {
        if (this.onUnload != null) {
            this.onUnload.run();
        }
    }

    public ChunkImageFactory getChunkImageFactory() {
        if (imageFactory == null) {
            imageFactory = new ChunkImageFactory(this);
        }
        return imageFactory;
    }

    public CoordinateDim2D getLocation() {
        return location;
    }

    public void updateBlock(Coordinate3D coords, int blockStateId) {
        updateBlock(coords, blockStateId, false);
    }

    public void updateBlock(Coordinate3D coords, int blockStateId, boolean suppressUpdate) {
        int section = coords.getY() / SECTION_HEIGHT;

        // if there's no section, we create an empty one
        if (chunkSections[section] == null) {
            chunkSections[section] = createNewChunkSection((byte) section, Palette.empty());
            chunkSections[section].setBlocks(new long[256]);
        }
        chunkSections[section].setBlockAt(coords.chunkLocalToSectionLocal(), blockStateId);

        if (suppressUpdate) {
            return;
        }

        if (this.imageFactory != null) {
            this.imageFactory.updateHeight(coords);
        }
    }

    /**
     * Update a number of blocks. toUpdate keeps track of which blocks have changed so that we can only redraw the
     * chunk if that's actually needed.
     * @param pos
     * @param provider
     */
    public void updateBlocks(Coordinate3D pos, DataTypeProvider provider) {
        int count = provider.readVarInt();
        Collection<Coordinate3D> toUpdate = new ArrayList<>();
        while (count-- > 0) {
            byte xz = provider.readNext();
            int y = provider.readNext();
            int x = (xz >>> 4) & 0x0F;
            int z = xz & 0x0F;

            int blockId = provider.readVarInt();

            Coordinate3D blockPos = new Coordinate3D(x, y, z);
            toUpdate.add(blockPos);

            updateBlock(blockPos, blockId, true);
        }
        this.getChunkImageFactory().recomputeHeights(toUpdate);
    }

    public void updateLight(DataTypeProvider provider) {
        this.isLit = true;
    };

    public ChunkState getState() {
        return new ChunkState(true, isSaved(), isLit());
    }

    public boolean isLit() {
        return this.isLit;
    }

    public void setLit(boolean lit) {
        this.isLit = lit;
    }
}