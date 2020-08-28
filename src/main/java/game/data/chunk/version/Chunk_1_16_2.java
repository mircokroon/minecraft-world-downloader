package game.data.chunk.version;

import game.data.CoordinateDim2D;
import packets.DataTypeProvider;

public class Chunk_1_16_2 extends Chunk_1_16 {
    public Chunk_1_16_2(CoordinateDim2D location) {
        super(location);
    }

    // this was introduced in 1.16.0 but is already gone in 1.16.2
    @Override
    protected void readIgnoreOldData(DataTypeProvider dataProvider) { }

    // 1.16.2 changes biomes from int[1024] to varint[given length]
    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        int biomesLength = provider.readVarInt();
        setBiomes(provider.readVarIntArray(biomesLength));
    }
}
