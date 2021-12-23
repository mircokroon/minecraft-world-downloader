package game.data.chunk.palette;

import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

import java.util.List;

public class DirectPalette extends Palette {
    int bitsPerBlock;

    public DirectPalette() {
        this(GlobalPaletteProvider.getGlobalPalette().getRequiredBits());
    }

    public DirectPalette(int bitsPerBlock) {
        super();

        this.bitsPerBlock = bitsPerBlock;
    }

    @Override
    public int stateFromId(int index) {
        return index;
    }

    @Override
    public int getBitsPerBlock() {
        return bitsPerBlock;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<SpecificTag> toNbt() {
        return List.of();
    }

    @Override
    public void write(PacketBuilder packet) {
        packet.writeByte((byte) bitsPerBlock);
    }

    @Override
    public String toString() {
        return "DirectPalette{" +
                "bitsPerBlock=" + bitsPerBlock +
                '}';
    }
}
