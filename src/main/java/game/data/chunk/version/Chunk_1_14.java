package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;

public class Chunk_1_14 extends Chunk_1_13 {

    SpecificTag heightMap;

    public Chunk_1_14(int x, int z) {
        super(x, z);
    }

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("Heightmaps", heightMap);
        super.addLevelNbtTags(map);
    }

    @Override
    protected void readBlockCount(DataTypeProvider provider) {
        int blockCount = provider.readShort();
    }

    @Override
    protected void parseLights(ChunkSection section, DataTypeProvider dataProvider) {
        // no lights here in 1.14+
    }

    @Override
    protected void parseHeightMaps(DataTypeProvider dataProvider) {
        heightMap = dataProvider.readNbtTag();
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette, int bitsPerBlock) {
        return new ChunkSection_1_14(y, palette);
    }
}
