package game.data.chunk.version;

import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

public class Chunk_1_16 extends Chunk_1_15 {
    private boolean ignoreOldData;

    public Chunk_1_16(int x, int z) {
        super(x, z);
    }

    @Override
    protected void readIgnoreOldData(DataTypeProvider dataProvider) {
        ignoreOldData = dataProvider.readBoolean();
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_16(y, palette);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_16(sectionY, section);
    }
}
