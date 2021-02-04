package game.data.chunk.version;

import game.data.chunk.palette.Palette;
import se.llbit.nbt.Tag;

public class ChunkSection_1_16_2 extends ChunkSection_1_16 {
    public ChunkSection_1_16_2(byte y, Palette palette) {
        super(y, palette);
    }

    public ChunkSection_1_16_2(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }

    @Override
    public int getDataVersion() {
        return Chunk_1_16_2.DATA_VERSION;
    }
}
