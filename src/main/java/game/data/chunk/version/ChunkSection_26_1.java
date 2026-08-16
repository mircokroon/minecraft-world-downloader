package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import packets.builder.PacketBuilder;
import se.llbit.nbt.Tag;

/**
 * As of protocol 770 (1.21.5), chunk sections gained a "fluid count" short right after the block count, and the
 * paletted container data arrays (blocks and biomes) are no longer length-prefixed on the wire - the length is
 * calculated from bits-per-entry instead. See Chunk_26_1#readChunkColumn for the read side of this.
 */
public class ChunkSection_26_1 extends ChunkSection_1_18 {
    public ChunkSection_26_1(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }

    public ChunkSection_26_1(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, nbt, chunk);
    }

    @Override
    public void write(PacketBuilder packet) {
        if (blockCount < 0) { blockCount = palette.isEmpty() ? 0 : 4096; }

        packet.writeShort(blockCount);
        packet.writeShort(0); // fluid count - not tracked separately, 0 is a safe placeholder

        palette.write(packet);
        packet.writeLongArray(blocks);

        biomePalette.write(packet);
        packet.writeLongArray(biomes);
    }
}
