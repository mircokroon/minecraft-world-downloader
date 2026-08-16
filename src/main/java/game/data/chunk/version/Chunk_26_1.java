package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.chunk.palette.PaletteType;
import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.LongArrayTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import static util.PrintUtils.devPrint;

/**
 * As of protocol 775 (26.1):
 * - heightmaps in the chunk packet are no longer sent as an NBT compound, but as an explicit array of
 *   (type, long array) pairs. We still store them internally as the same NBT compound used by older versions
 *   (and by the on-disk chunk format), so only the network (de)serialization changes here.
 * - chunk sections (see ChunkSection_26_1) gained a "fluid count" field and their paletted container data arrays
 *   are no longer length-prefixed on the wire.
 */
public class Chunk_26_1 extends Chunk_1_20 {
    private static final String[] HEIGHTMAP_TYPES = {
        "WORLD_SURFACE_WG",
        "WORLD_SURFACE",
        "OCEAN_FLOOR_WG",
        "OCEAN_FLOOR",
        "MOTION_BLOCKING",
        "MOTION_BLOCKING_NO_LEAVES"
    };

    public Chunk_26_1(CoordinateDim2D location, int version) {
        super(location, version);
    }

    @Override
    protected void parseHeightMaps(DataTypeProvider dataProvider) {
        CompoundTag tag = new CompoundTag();

        int count = dataProvider.readVarInt();
        for (int i = 0; i < count; i++) {
            int type = dataProvider.readVarInt();
            int longCount = dataProvider.readVarInt();
            long[] data = dataProvider.readLongArray(longCount);

            String name = type >= 0 && type < HEIGHTMAP_TYPES.length ? HEIGHTMAP_TYPES[type] : "UNKNOWN_" + type;
            tag.add(name, new LongArrayTag(data));

            devPrint("[heightmap] " + location + " type=" + name + " (" + type + ") longs=" + data.length);
        }

        heightMap = tag;
    }

    @Override
    protected void writeHeightMaps(PacketBuilder packet) {
        CompoundTag tag = heightMap != null ? heightMap.asCompound() : new CompoundTag();

        packet.writeVarInt(tag.size());
        for (NamedTag entry : tag) {
            packet.writeVarInt(indexOfType(entry.name()));

            long[] data = entry.getTag().longArray();
            packet.writeVarInt(data.length);
            packet.writeLongArray(data);
        }
    }

    private int indexOfType(String name) {
        for (int i = 0; i < HEIGHTMAP_TYPES.length; i++) {
            if (HEIGHTMAP_TYPES[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_26_1(y, palette, this);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_26_1(sectionY, section, this);
    }

    /**
     * Same as Chunk_1_18#readChunkColumn, except: a "fluid count" short follows the block count, and the block/biome
     * data arrays are no longer length-prefixed - their length is derived from bits-per-entry instead.
     */
    @Override
    public void readChunkColumn(DataTypeProvider dataProvider) {
        for (int sectionY = getMinBlockSection(); sectionY <= getMaxBlockSection() && dataProvider.hasNext(); sectionY++) {
            ChunkSection_1_18 section = (ChunkSection_1_18) getChunkSection(sectionY);

            int blockCount = dataProvider.readShort();
            dataProvider.readShort(); // fluid count, not tracked separately

            Palette blockPalette = Palette.readPalette(dataProvider, PaletteType.BLOCKS);

            if (section == null) {
                section = (ChunkSection_1_18) createNewChunkSection((byte) (sectionY & 0xFF), blockPalette);
            } else {
                section.setBlockPalette(blockPalette);
            }

            section.setBlockCount(blockCount);
            section.setBlocks(dataProvider.readLongArray(ChunkSection_1_18.longsRequired(blockPalette.getBitsPerBlock())));

            Palette biomePalette = Palette.readPalette(dataProvider, PaletteType.BIOMES);
            section.setBiomePalette(biomePalette);
            section.setBiomes(dataProvider.readLongArray(ChunkSection_1_18.longsRequiredBiomes(biomePalette.getBitsPerBlock())));

            setChunkSection(sectionY, section);

            if (containsBlockEntities(blockPalette)) {
                findBlockEntities(section, sectionY);
            }
        }
    }
}
