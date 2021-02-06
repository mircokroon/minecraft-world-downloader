package game.data.chunk.version;

import game.data.CoordinateDim2D;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;
import util.ToString;

import java.util.Arrays;

/**
 * Chunks in the 1.12(.2) format. Biomes were a byte array in this version.
 */
public class Chunk_1_12 extends Chunk {
    public static final int DATA_VERSION = 1132;

    private byte[] biomes;

    public Chunk_1_12(CoordinateDim2D location) {
        super(location);

        this.biomes = new byte[256];
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_12(y, palette);
    }

    private void setBiome(int x, int z, byte biomeId) {
        biomes[z * 16 + x] = biomeId;
    }

    @Override
    protected void parse2DBiomeData(DataTypeProvider dataProvider) {
        for (int z = 0; z < SECTION_WIDTH; z++) {
            for (int x = 0; x < SECTION_WIDTH; x++) {
                setBiome(x, z, dataProvider.readNext());
            }
        }
    }

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("TerrainPopulated", new ByteTag((byte) 1));
        map.add("LightPopulated", new ByteTag((byte) 1));

        super.addLevelNbtTags(map);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_12(sectionY, section);
    }

    protected SpecificTag getNbtBiomes() {
        return new ByteArrayTag(biomes);
    }

    @Override
    protected void parseBiomes(Tag tag) {
        this.biomes = tag.get("Level").asCompound().get("Biomes").byteArray();
    }

    @Override
    protected PacketBuilder writeSectionData() {
        PacketBuilder column = super.writeSectionData();
        column.writeByteArray(biomes);
        return column;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Chunk_1_12 that = (Chunk_1_12) o;

        return Arrays.equals(biomes, that.biomes);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(biomes);
        return result;
    }

    @Override
    public String toString() {
        return "Chunk_1_12{" + super.toString() +
                "\nlocation=" + location +
                "\nbiomes=" + ToString.array(biomes) +
                '}';
    }
}
