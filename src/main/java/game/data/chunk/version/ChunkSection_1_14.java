package game.data.chunk.version;

import config.Version;
import game.data.chunk.Chunk;
import game.data.chunk.palette.Palette;
import packets.builder.PacketBuilder;
import se.llbit.nbt.Tag;

public class ChunkSection_1_14 extends ChunkSection_1_13 {
    public static final Version VERSION = Version.V1_14;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    public ChunkSection_1_14(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
    }

    public ChunkSection_1_14(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, nbt, chunk);
    }

    @Override
    public void write(PacketBuilder packet) {
        packet.writeShort(4096);
        palette.write(packet);

        packet.writeVarInt(blocks.length);
        packet.writeLongArray(blocks);
    }
}
