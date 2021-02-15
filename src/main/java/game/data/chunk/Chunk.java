package game.data.chunk;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.GlobalPaletteProvider;
import game.data.chunk.palette.Palette;
import game.data.chunk.palette.SimpleColor;
import game.data.container.InventoryWindow;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.dimension.Dimension;
import game.data.entity.Entity;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;
import util.PrintUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Basic chunk class. May be extended by version-specific ones as they can have implementation differences.
 */
public abstract class Chunk {
    protected static final int LIGHT_SIZE = 2048;
    protected static final int CHUNK_HEIGHT = 256;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_WIDTH = 16;

    public CoordinateDim2D location;
    private final Map<Coordinate3D, SpecificTag> tileEntities;
    private final Set<Entity> entities;

    protected ChunkSection[] getChunkSections() {
        return chunkSections;
    }

    private final ChunkSection[] chunkSections;

    private Runnable afterParse;
    private boolean isNewChunk;

    private boolean saved;

    private int[] heightMap;

    public Chunk(CoordinateDim2D location) {
        this.saved = false;
        this.location = location;
        this.isNewChunk = false;

        chunkSections = new ChunkSection[16];
        tileEntities = new HashMap<>();
        entities = new HashSet<>();
    }
    public abstract int getDataVersion();

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

        // validate entity identifer
        if (!entity.get("id").isError()) {
            String id = entity.get("id").stringValue();

            // invalid identifier - some servers will send these and it makes Minecraft angry when we load the world
            if (!id.matches("^[a-z0-9/._-]*$")) {
                entity.add("id", new StringTag(id.toLowerCase()));
            }
        }

        // get offset location
        Coordinate3D offset = location.offsetGlobal();

        // insert new coordinates (offset)
        entity.add("x", new IntTag(offset.getX()));
        entity.add("y", new IntTag(offset.getY()));
        entity.add("z", new IntTag(offset.getZ()));

        tileEntities.put(location, tag);

        // check for inventory contents we previously saved
        CoordinateDim3D pos = location.addDimension3D(this.location.getDimension());
        WorldManager.getInstance().getContainerManager().loadPreviousInventoriesAt(this, pos);
    }

    /**
     * Add a tile entity, if the position is not known yet. This method will also offset the position.
     * @param nbtTag the NBT data of the tile entity, should include X, Y, Z of the entity
     */
    private void addTileEntity(SpecificTag nbtTag) {
        CompoundTag entity = (CompoundTag) nbtTag;
        Coordinate3D position = new Coordinate3D(entity.get("x").intValue(), entity.get("y").intValue(), entity.get("z").intValue());

        addTileEntity(position, nbtTag);
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
    protected void parseHeightMaps(DataTypeProvider dataProvider) { }
    protected void readBlockCount(DataTypeProvider provider) { }
    protected abstract ChunkSection createNewChunkSection(byte y, Palette palette);
    protected abstract SpecificTag getNbtBiomes();

    protected void parse2DBiomeData(DataTypeProvider provider) { }
    protected void parse3DBiomeData(DataTypeProvider provider) { }

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
        Coordinate2D location = this.location.offsetChunk();
        map.add("xPos", new IntTag(location.getX()));
        map.add("zPos", new IntTag(location.getZ()));

        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>()));

        map.add("Biomes", getNbtBiomes());
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(tileEntities.values())));
        map.add("Sections", new ListTag(Tag.TAG_COMPOUND, getSectionList()));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, getEntityList()));
    }

    private List<SpecificTag> getEntityList() {
        return entities.stream().filter(Objects::nonNull).map(Entity::toNbt).collect(Collectors.toList());
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

        if (full) {
            parse3DBiomeData(dataProvider);
        }

        int size = dataProvider.readVarInt();
        readChunkColumn(full, mask, dataProvider.ofLength(size));

        // Used to generate overview, not replaced by 1.14 NBT height maps
        computeHeightMap();

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
        if (chunkSections[section] == null) { return 0; }

        return chunkSections[section].getNumericBlockStateAt(x, y % SECTION_HEIGHT, z);
    }

    public BlockState getBlockStateAt(Coordinate3D location) {
        return getBlockStateAt(location.getX(), location.getY(), location.getZ());
    }

    public BlockState getBlockStateAt(int x, int y, int z) {
        int id = getNumericBlockStateAt(x, y, z);
        if (id == 0) { return null; }

        return GlobalPaletteProvider.getGlobalPalette(getDataVersion()).getState(id);
    }

    /**
     * Generate network packet for this chunk.
     */
    public PacketBuilder toPacket() {
        Protocol p = ProtocolVersionHandler.getInstance().getProtocolByProtocolVersion(Config.getProtocolVersion());
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

        columns.build();

        // we don't include block entities - these chunks will be far away so they shouldn't be rendered anyway
        packet.writeVarInt(0);
        return packet;
    }

    protected void writeHeightMaps(PacketBuilder packet) { }

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

    protected void writeBiomes(PacketBuilder packet) { };


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

    protected void computeHeightMap() {
        heightMap = new int[SECTION_WIDTH * SECTION_WIDTH];

        for (int x = 0; x < SECTION_WIDTH; x++) {
            for (int z = 0; z < SECTION_WIDTH; z++) {
                heightMap[z << 4 | x] = computeHeight(x, z);
            }
        }
    }

    private int computeHeight(int x, int z) {
        // if we're in the Nether, we only consider blocks below Y=96. Otherwise the entire minimap is grey.
        int topSection = this.location.getDimension().equals(Dimension.NETHER) ? 5 : 15;

        for (int chunkSection = topSection; chunkSection >= 0; chunkSection--) {

            ChunkSection cs = getChunkSections()[chunkSection];
            if (cs == null) { continue; }

            int height = cs.height(x, z);

            if (height < 0) { continue; }

            return (chunkSection * SECTION_HEIGHT) + height;
        }
        return 0;
    }

    public int heightAt(int x, int z) {
        return heightMap[z << 4 | x];
    }


    /**
     * Generate and return the overview image for this chunk.
     */
    public Image getImage() {
        WritableImage i = new WritableImage(16, 16);
        PixelWriter writer = i.getPixelWriter();

        try {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = heightAt(x, z);
                    BlockState blockState = getBlockStateAt(x, heightAt(x, z), z);

                    SimpleColor color;
                    if (blockState == null) {
                        color = SimpleColor.TRANSPARENT;
                    } else {
                        color = blockState.getColor();
                        for (int offset = 1; offset < 24 && blockState.isWater(); offset++) {
                            // make sure the offset doesn't put us into the negatives
                            if (y - offset < 0) {
                                break;
                            }

                            blockState = getBlockStateAt(x, y - offset, z);
                            if (blockState == null) {
                                break;
                            }
                            color = color.blendWith(blockState.getColor(), 1.1 - (0.6 / Math.sqrt(offset)));
                        }
                    }

                    color = color.shaderMultiply(getColorShader(x, z));



                    writer.setColor(x, z, color.toJavaFxColor());

                    // mark new chunks in a red-ish outline
                    if (isNewChunk() && ((x == 0 || x == 15) || (z == 0 || z == 15))) {
                        writer.setColor(x, z, color.highlight().toJavaFxColor());
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Unable to draw picture for chunk at " + this.location);
            ex.printStackTrace();
        }

        return i;
    }

    /**
     * Looks at the block one coordinate north of the current to check if its above or below the current.
     * @return a colour multiplier to adjust the color value by. If they elevations are the same it will be 1.0, if the
     * northern block is above the current its 0.8, otherwise its 1.2.
     */
    private double getColorShader(int x, int z) {
        int ySelf = heightAt(x, z);

        int yNorth;
        if (z == 0) {
            CoordinateDim2D coordinate = location.copy();
            coordinate.offset(0, -1);

            Chunk other = WorldManager.getInstance().getChunk(coordinate);

            if (other == null) { return 1; }
            else { yNorth = other.heightAt(x, 15); }
        } else {
            yNorth = heightAt(x, z - 1);
        }

        if (ySelf < yNorth) {
            return 0.4 + (0.4 / (yNorth - ySelf));
        } else if (ySelf > yNorth) {
            return 1.9 - (0.7 / Math.sqrt(ySelf - yNorth));
        }
        return 1;
    }

    public void addEntity(Entity ent) {
        entities.add(ent);
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

        computeHeightMap();
    }

    protected void parseHeightMaps(Tag tag) { }
    protected void parseBiomes(Tag tag) { }
    protected abstract ChunkSection parseSection(int sectionY, SpecificTag section);

    /**
     * Add inventory items to a tile entity (e.g. a chest)
     */
    public void addInventory(InventoryWindow window) {
        CompoundTag tileEntity = (CompoundTag) tileEntities.get(window.getContainerLocation());

        // if a tile entity is missing, don't store anything
        if (tileEntity == null) {
            return;
        }

        tileEntity.add("Items", new ListTag(Tag.TAG_COMPOUND, window.getSlotsNbt()));

        if (window.hasCustomName()) {
            tileEntity.add("CustomName", new StringTag(window.getWindowTitle()));
        }

    }

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
        return Arrays.equals(heightMap, chunk.heightMap);
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(chunkSections);
        result = 31 * result + Arrays.hashCode(heightMap);
        return result;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "location=" + location +
                ", tileEntities=" + tileEntities +
                ", entities=" + entities +
                ", chunkSections=" + Arrays.toString(chunkSections) +
                ", heightMap=" + PrintUtils.array(heightMap) +
                '}';
    }
}
