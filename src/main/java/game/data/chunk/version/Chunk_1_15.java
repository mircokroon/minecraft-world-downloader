package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

public class Chunk_1_15 extends Chunk_1_14 {
    public Chunk_1_15(CoordinateDim2D location, int version) {
        super(location, version);
    }

    @Override
    protected void parse2DBiomeData(DataTypeProvider dataProvider) { }

    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        setBiomes(provider.readIntArray(1024));
    }

    @Override
    public ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_15(y, palette, this);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_15(sectionY, section, this);
    }

    @Override
    protected void writeSectionDataBiomes(PacketBuilder builder) { }

    @Override
    protected void writeBiomes(PacketBuilder packet) {
        int[] biomes = getBiomes();
        packet.writeIntArray(biomes);
    }
}
