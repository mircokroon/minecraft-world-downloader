package game.data.chunk.version;

import game.data.CoordinateDim2D;
import game.data.chunk.ChunkSection;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

/**
 * In 1.14 the chunks are now given a heightmap in the packet. They also no longer contain light information, as
 * this was moved to a different packet. Also, a block count?
 */
public class Chunk_1_14 extends Chunk_1_13 {
    public static final int DATA_VERSION = 1901;

    SpecificTag heightMap;

    public Chunk_1_14(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        super.addLevelNbtTags(map);

        map.add("Heightmaps", heightMap);
        map.add("Status", new StringTag("full"));
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
}
