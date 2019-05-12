package game.data.chunk;

import packets.DataTypeProvider;

public class Palette {
    private static boolean maskBedrock = false;
    int bitsPerBlock;
    int[] palette;

    public static void setMaskBedrock(boolean maskBedrock) {
        Palette.maskBedrock = maskBedrock;
    }


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

        if (maskBedrock) {
            for (int i = 0; i < palette.length; i++) {
                if (palette[i] == 0x70) {
                    palette[i] = 0x10;
                }
            }
        }

        return new Palette(bitsPerBlock, palette);
    }

    public BlockState StateForId(int data) {
        return new BlockState(palette[data]);
    }
}
