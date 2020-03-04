package game.data.chunk.version;

import game.data.chunk.palette.Palette;
import se.llbit.nbt.Tag;

/**
 * No changes here over 1.13.
 */
public class ChunkSection_1_14 extends ChunkSection_1_13 {
    public ChunkSection_1_14(byte y, Palette palette) {
        super(y, palette);
    }
    public ChunkSection_1_14(int sectionY, Tag nbt) { super(sectionY, nbt); }
}
