package game.data.chunk.version;

import game.data.CoordinateDim2D;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

public class Chunk_1_16_2 extends Chunk_1_16 {
    public static final int DATA_VERSION = 2578;

    public Chunk_1_16_2(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }

    // this was introduced in 1.16.0 but is already gone in 1.16.2
    @Override
    protected void readIgnoreOldData(DataTypeProvider dataProvider) { }

    // 1.16.2 changes biomes from int[1024] to varint[given length]
    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        int biomesLength = provider.readVarInt();
        setBiomes(provider.readVarIntArray(biomesLength));
    }

    @Override
    protected void writeBiomes(PacketBuilder packet) {
        int[] biomes = getBiomes();
        packet.writeVarInt(biomes.length);
        packet.writeVarIntArray(biomes);
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_16_2(y, palette);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_16_2(sectionY, section);
    }
}
