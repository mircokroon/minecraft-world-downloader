package game.data.chunk.version;

import game.data.chunk.palette.Palette;
import packets.builder.PacketBuilder;
import se.llbit.nbt.Tag;

public class ChunkSection_1_14 extends ChunkSection_1_13 {
    public ChunkSection_1_14(byte y, Palette palette) {
        super(y, palette);
    }

    public ChunkSection_1_14(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }

    @Override
    public int getDataVersion() {
        return Chunk_1_14.DATA_VERSION;
    }

    @Override
    public void write(PacketBuilder packet) {
        packet.writeShort(4096);
        palette.write(packet);

        packet.writeVarInt(blocks.length);
        packet.writeLongArray(blocks);
    }
}
