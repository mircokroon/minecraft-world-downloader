package game;

import packets.DataTypeProvider;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Palette {
    int bitsPerBlock;
    int[] palette;

    private Palette(int bitsPerBlock, int[] palette) {
        this.bitsPerBlock = bitsPerBlock;
        this.palette = palette;

        if (bitsPerBlock > 8) {
            System.out.println("WARNING: palette type not supported");
        }
    }

    public static Palette readPalette(int bitsPerBlock, DataTypeProvider dataTypeProvider) {
        int size = dataTypeProvider.readVarInt();

        int[] palette = dataTypeProvider.readVarIntArray(size);

        return new Palette(bitsPerBlock, palette);
    }

    public BlockState StateForId(int data) {
        return new BlockState(palette[data]);
    }
}
