package game.data.chunk.version;

import packets.DataTypeProvider;

public class Chunk_1_15 extends Chunk_1_14 {
    public Chunk_1_15(int x, int z) {
        super(x, z);
    }

    @Override
    protected void parse2DBiomeData(DataTypeProvider dataProvider) { }

    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        setBiomes(provider.readIntArray(1024));
    }
}
