package game.data.chunk.version;

import config.Version;
import game.data.chunk.IncompleteChunkException;
import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;

import java.util.Arrays;

/**
 * Chunk format for 1.13+. Now includes a status tag and the biomes are integers.
 */
public class Chunk_1_13 extends Chunk {
    public static final Version VERSION = Version.V1_13;

    @Override
    public int getDataVersion() { return VERSION.dataVersion; }

    private int[] biomes;

    public Chunk_1_13(CoordinateDim2D location) {
        super(location);
    }


    @Override
    public ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_13(y, palette, this);
    }

    @Override
    protected void parse2DBiomeData(DataTypeProvider dataProvider) {
        setBiomes(dataProvider.readIntArray(256));
    }


    protected void setBiomes(int[] biomes) {
        this.biomes = biomes;
    }

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("Status", new StringTag("postprocessed"));

        // empty maps
        CompoundTag carvingMasks = new CompoundTag();
        carvingMasks.add("AIR", new ByteArrayTag(new byte[0]));
        carvingMasks.add("LIQUID", new ByteArrayTag(new byte[0]));
        map.add("CarvingMasks", carvingMasks);

        CompoundTag structures = new CompoundTag();
        structures.add("References", new CompoundTag());
        structures.add("Starts", new CompoundTag());
        map.add("Structures", structures);

        super.addLevelNbtTags(map);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_13(sectionY, section);
    }


    protected SpecificTag getNbtBiomes() {
        return new IntArrayTag(biomes);
    }

    @Override
    protected void parseBiomes(Tag tag) {
        Tag biomeTag = tag.get("Level").asCompound().get("Biomes");
        this.biomes = biomeTag.intArray();
    }

    protected int[] getBiomes() {
        return biomes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Chunk_1_13 that = (Chunk_1_13) o;

        return Arrays.equals(biomes, that.biomes);
    }

    @Override
    protected PacketBuilder writeSectionData() {
        PacketBuilder parent = super.writeSectionData();
        writeSectionDataBiomes(parent);
        return parent;
    }

    protected void writeSectionDataBiomes(PacketBuilder builder) {
        if (this.biomes.length == 0) {
            throw new IncompleteChunkException();
        }
        builder.writeIntArray(getBiomes());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(biomes);
        return result;
    }
}
