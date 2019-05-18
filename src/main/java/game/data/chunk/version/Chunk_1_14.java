package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import packets.DataTypeProvider;

public class Chunk_1_14 extends Chunk_1_13 {
    public Chunk_1_14(int x, int z) {
        super(x, z);
        throw new RuntimeException("1.14 chunk parsing is not currently supported :(");
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
    protected ChunkSection createNewChunkSection(byte y, Palette palette, int bitsPerBlock) {
        return new ChunkSection_1_14(y, palette);
    }
}
