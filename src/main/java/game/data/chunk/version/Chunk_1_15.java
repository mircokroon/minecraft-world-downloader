package game.data.chunk.version;

import game.data.CoordinateDim2D;
import packets.DataTypeProvider;

public class Chunk_1_15 extends Chunk_1_14 {
    public static final int DATA_VERSION = 2200;

    public Chunk_1_15(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }

    @Override
    protected void parse2DBiomeData(DataTypeProvider dataProvider) { }

    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        setBiomes(provider.readIntArray(1024));
    }
}
