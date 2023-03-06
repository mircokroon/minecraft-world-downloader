package game.data.chunk.version;

import config.Version;
import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.Tag;

public class ChunkSection_1_17 extends ChunkSection_1_16 {
    public static final Version VERSION = Version.V1_17;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    public ChunkSection_1_17(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }

    public ChunkSection_1_17(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, nbt, chunk);
    }
}
