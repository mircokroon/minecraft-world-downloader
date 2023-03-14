package game.data.chunk;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import game.data.chunk.palette.PaletteType;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.protocol.Protocol;
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
    private final ChunkSection[] chunkSections;
    public CoordinateDim2D location;
    private Runnable afterParse;
    private Runnable onUnload;
    private boolean isNewChunk;
    private boolean saved;
    private ChunkImageFactory imageFactory;

    private final int dataVersion;

    public Chunk(CoordinateDim2D location, int dataVersion) {
        super();

        this.dataVersion = dataVersion;
        this.saved = false;
        this.location = location;
        this.isNewChunk = false;

        chunkSections = new ChunkSection[getMaxLightSection() - getMinLightSection() + 1];
    }

    protected ChunkSection getChunkSection(int y) {
        if (y < getMinLightSection()) { return null; }
        if (y > getMaxLightSection()) { return null; }

        return chunkSections[y - getMinLightSection()];
    }

    protected void setChunkSection(int y, ChunkSection section) {
        if (y < getMinLightSection()) { return; }
        if (y > getMaxLightSection()) { return; }

        chunkSections[y - getMinLightSection()] = section;
    }

    protected int getMinLightSection() {
        return 0;
    }

    protected int getMinBlockSection() {
        return 0;
    }

    protected int getMaxLightSection() {
        return 15;
    }

    protected int getMaxBlockSection() {
        return 15;
    }

    protected Iterable<ChunkSection> getAllSections() {
        return () -> Arrays.stream(chunkSections).filter(Objects::nonNull).iterator();
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        if (saved && !this.saved) {
            getChunkImageFactory().markSaved();
        }
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
    public void readChunkColumn(boolean full, BitSet mask, DataTypeProvider dataProvider) {
        // Loop through section Y values, starting from the lowest section that has blocks inside it. Compute the index
        // in the mask by subtracting the minimum chunk packet section, e.g. the lowest Y value we will find in the
        // mask.
        for (int sectionY = getMinBlockSection(); sectionY <= getMaxLightSection() && !mask.isEmpty(); sectionY++) {
            ChunkSection section = getChunkSection(sectionY);

            // A 1 in the position of the mask indicates this chunk section is present in the data buffer
            int maskIndex = sectionY - getMinBlockSection();
            if (!mask.get(maskIndex)) {
                if (full && section != null) {
                    section.resetBlocks();
                }
                continue;
            }
            // Set indices to 0 when read so that we can stop once the mask is empty
            mask.set(maskIndex, false);

            readBlockCount(dataProvider);

            byte bitsPerBlock = dataProvider.readNext();
            Palette palette = Palette.readPalette(bitsPerBlock, dataProvider, PaletteType.BLOCKS);

            // A bitmask that contains bitsPerBlock set bits
            int dataArrayLength = dataProvider.readVarInt();

            if (section == null) {
                section = createNewChunkSection((byte) (sectionY & 0xFF), palette);
            }

            // if the section has no blocks
            if (dataArrayLength == 0) {
                continue;
            }

            // parse blocks
            section.setBlocks(dataProvider.readLongArray(dataArrayLength));

            parseLights(section, dataProvider);

            // May replace an existing section or a null one
            setChunkSection(sectionY, section);
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

    public abstract ChunkSection createNewChunkSection(byte y, Palette palette);

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

    protected boolean hasSections() {
        return getAllSections().iterator().hasNext();
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

        addGeneralLevelTags(map);

        map.add("Biomes", getNbtBiomes());
        map.add("Sections", new ListTag(Tag.TAG_COMPOUND, getSectionList()));
    }

    protected void addGeneralLevelTags(CompoundTag map) {
        Coordinate2D offset = this.location.offsetChunk();
        map.add("xPos", new IntTag(offset.getX()));
        map.add("zPos", new IntTag(offset.getZ()));

        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));
    }

    /**
     * Get a list of section tags for the NBT.
     */
    protected List<SpecificTag> getSectionList() {
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
     */
    protected void parse(DataTypeProvider dataProvider) {
        raiseEvent("parse from packet");

        boolean full = dataProvider.readBoolean();
        // if we don't have the partial chunk (anymore?), just make one from scratch
        if (!full) {
            this.markAsNew();
        }

        // for older versions, we use a BitSet as 1.17+ does. We construct it manually by turning the single int into
        // a long.
        long maskLong = dataProvider.readVarInt();
        BitSet mask = BitSet.valueOf(new long[]{maskLong});

        // for 1.14+
        parseHeightMaps(dataProvider);

        if (full) {
            parse3DBiomeData(dataProvider);
        }

        int size = dataProvider.readVarInt();
        readChunkColumn(full, mask, dataProvider.ofLength(size));

        parseBlockEntities(dataProvider);
        afterParse();
    }

    protected void parseBlockEntities(DataTypeProvider dataProvider) {
        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            addBlockEntity(dataProvider.readNbtTag());
        }
    }

    protected void afterParse() {
        // ensure the chunk is (re)saved
        this.saved = false;

        // run the callback if one exists
        if (afterParse != null) {
            afterParse.run();
        }
    }


    public int getNumericBlockStateAt(int x, int y, int z) {
        int sectionY = (int) Math.floor((double) y / SECTION_HEIGHT);
        ChunkSection section = getChunkSection(sectionY);
        if (section == null) {
            return 0;
        }

        return section.getNumericBlockStateAt(x, Math.floorMod(y, SECTION_HEIGHT), z);
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
        packet.writeVarInt(p.clientBound("LevelChunk"));

        packet.writeInt(location.getX());
        packet.writeInt(location.getZ());
        packet.writeBoolean(true);

        writeBitMask(packet);
        writeHeightMaps(packet);
        writeBiomes(packet);
        writeChunkSections(packet);

        // we don't include block entities - these chunks will be far away so they shouldn't be rendered anyway
        packet.writeVarInt(0);
        return packet;
    }

    protected void writeChunkSections(PacketBuilder packet) {
        PacketBuilder columns = writeSectionData();
        byte[] columnArr = columns.toArray();
        packet.writeVarInt(columnArr.length);
        packet.writeByteArray(columnArr);
    }

    public PacketBuilder toLightPacket() { return null; }

    protected void writeHeightMaps(PacketBuilder packet) {
    }

    protected PacketBuilder writeSectionData() {
        PacketBuilder column = new PacketBuilder();
        for (ChunkSection section : getAllSections()) {
            if (section.getY() >= getMinBlockSection()) {
                section.write(column);
            }
        }

        return column;
    }

    private void writeBitMask(PacketBuilder packet) {
        int res = 0;
        for (ChunkSection section : getAllSections()) {
            if (section.getY() >= getMinBlockSection()) {
                res |= 1 << section.getY() - getMinBlockSection();
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
        raiseEvent("parse from nbt");

        tag.get("Level").asCompound().get("Sections").asList().forEach(section -> {
            int sectionY = section.get("Y").byteValue();
            setChunkSection(sectionY, parseSection(sectionY, section));
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
            "dataVersion=" + dataVersion +
            ", location=" + location +
            ", chunkSections=" + Arrays.toString(chunkSections) +
            '}';
    }

    public void unload() {
        raiseEvent("unload");

        if (this.onUnload != null) {
            this.onUnload.run();
        }
    }

    public ChunkImageFactory getChunkImageFactory() {
        if (imageFactory == null) {
            // assignment should happen before running initialisation code
            imageFactory = new ChunkImageFactory(this);
            imageFactory.initialise();
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
        raiseEvent("update block");

        int sectionY = coords.getY() / SECTION_HEIGHT;

        // if there's no section, we create an empty one
        if (getChunkSection(sectionY) == null) {
            ChunkSection newChunkSection = createNewChunkSection((byte) sectionY, Palette.empty());
            newChunkSection.setBlocks(new long[256]);
            setChunkSection(sectionY, newChunkSection);
        }

        // if the section is still null, that means it's likely out of the world bounds so just ignore this update
        ChunkSection section = getChunkSection(sectionY);
        if (section == null) { return; }

        section.setBlockAt(coords.chunkLocalToSectionLocal(), blockStateId);

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
        raiseEvent("update lighting");
    }

    public boolean hasSeparateEntities() {
        return false;
    }
}