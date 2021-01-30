package game.data.chunk;

import game.Game;
import game.data.Coordinate3D;
import game.data.CoordinateDim2D;
import game.data.CoordinateDim3D;
import game.data.Dimension;
import game.data.WorldManager;
import game.data.chunk.entity.Entity;
import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.Palette;
import game.data.chunk.version.ColorTransformer;
import game.data.container.ContainerManager;
import game.data.container.InventoryWindow;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Basic chunk class. May be extended by version-specific ones as they can have implementation differences.
 */
public abstract class Chunk {
    private ColorTransformer colorTransformer;

    protected static final int LIGHT_SIZE = 2048;
    protected static final int CHUNK_HEIGHT = 256;
    public static final int SECTION_HEIGHT = 16;
    public static final int SECTION_WIDTH = 16;

    public CoordinateDim2D location;
    private Map<Coordinate3D, SpecificTag> tileEntities;
    private Set<Entity> entities;

    protected ChunkSection[] getChunkSections() {
        return chunkSections;
    }

    private ChunkSection[] chunkSections;

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
        colorTransformer = new ColorTransformer();
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

        // check for inventory contents we previously saved
        CoordinateDim3D pos = location.addDimension3D(this.location.getDimension());
        WorldManager.getContainerManager().loadPreviousInventoriesAt(this, pos);
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

                ChunkSection section = createNewChunkSection((byte) (sectionY & 0x0F), palette);

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

        // biome data is only present in full chunks, for <= 1.14.4
        if (full) {
            parse2DBiomeData(dataProvider);
        }
    }
    protected void parseHeightMaps(DataTypeProvider dataProvider) { }
    protected void readIgnoreOldData(DataTypeProvider dataProvider) { }
    protected void readBlockCount(DataTypeProvider provider) { }
    protected abstract ChunkSection createNewChunkSection(byte y, Palette palette);
    protected abstract SpecificTag getBiomes();

    protected void parse2DBiomeData(DataTypeProvider provider) { }
    protected void parse3DBiomeData(DataTypeProvider provider) { }

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
        map.add("xPos", new IntTag(this.location.getX()));
        map.add("zPos", new IntTag(this.location.getZ()));

        map.add("InhabitedTime", new LongTag(0));
        map.add("LastUpdate", new LongTag(0));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>()));

        map.add("Biomes", getBiomes());
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(tileEntities.values())));
        map.add("Sections", new ListTag(Tag.TAG_COMPOUND, getSectionList()));
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, getEntityList()));
    }

    private List<SpecificTag> getEntityList() {
        return entities.stream().map(Entity::toNbt).collect(Collectors.toList());
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
        // for 1.16+
        readIgnoreOldData(dataProvider);

        int mask = dataProvider.readVarInt();

        // for 1.14+
        parseHeightMaps(dataProvider);

        if (full) {
            parse3DBiomeData(dataProvider);
        }

        int size = dataProvider.readVarInt();

        readChunkColumn(full, mask, dataProvider);

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

        return WorldManager.getGlobalPalette().getState(id);
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

    public ColorTransformer getColorTransformer() {
        return colorTransformer;
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


    public Image getImage() {
        BufferedImage i = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);

        try {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int y = heightAt(x, z);
                    BlockState blockState = getBlockStateAt(x, heightAt(x, z), z);

                    int color;
                    if (blockState == null) {
                        color = 0;
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
                            color = getColorTransformer().blendWith(color, blockState.getColor(), 1.1 - (0.6 / Math.sqrt(offset)));
                        }
                    }

                    color = getColorTransformer().shaderMultiply(color, getColorShader(x, z));

                    i.setRGB(x, z, color);

                    // mark new chunks in a red-ish outline
                    if (isNewChunk() && ((x == 0 || x == 15) || (z == 0 || z == 15))) {
                        i.setRGB(x, z, getColorTransformer().highlight(i.getRGB(x, z)));
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

            Chunk other = WorldManager.getChunk(coordinate);

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
        computeHeightMap();
    }

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
}
