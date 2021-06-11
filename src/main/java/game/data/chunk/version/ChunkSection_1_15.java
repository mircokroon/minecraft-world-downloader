package game.data.chunk.version;

import config.Version;
import game.data.chunk.palette.Palette;
import packets.builder.PacketBuilder;
import se.llbit.nbt.Tag;

public class ChunkSection_1_15 extends ChunkSection_1_14 {
    public static final Version VERSION = Version.V1_15;
    @Override
    public int getDataVersion() {
        return VERSION.dataVersion;
    }

    public ChunkSection_1_15(byte y, Palette palette) {
        super(y, palette);
    }

    public ChunkSection_1_15(int sectionY, Tag nbt) {
        super(sectionY, nbt);
    }


    @Override
    public void write(PacketBuilder packet) {
        packet.writeShort(4096);
        palette.write(packet);

        packet.writeVarInt(blocks.length);
        packet.writeLongArray(blocks);
    }
}
