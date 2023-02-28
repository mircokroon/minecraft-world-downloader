package game.data.chunk.version;

import config.Version;
import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import se.llbit.nbt.Tag;

public class ChunkSection_1_19_3 extends ChunkSection_1_19 {
    public static final Version VERSION = Version.V1_19_3;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    public ChunkSection_1_19_3(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }

    public ChunkSection_1_19_3(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }
}
