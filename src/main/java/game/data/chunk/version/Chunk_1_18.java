package game.data.chunk.version;

import config.Version;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;
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
            // parse blocks
            section.setBlocks(dataProvider.readLongArray(dataProvider.readVarInt()));

            // biomes
            section.setBiomePalette(Palette.readPalette(dataProvider));
            section.setBiomes(dataProvider.readLongArray(dataProvider.readVarInt()));

            // May replace an existing section or a null one
            setChunkSection(sectionY, section);
        }
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

            // TODO: make tile entities work for 1.18
            // addTileEntity(dataProvider.readNbtTag());
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
