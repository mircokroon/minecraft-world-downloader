package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.GlobalPalette;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a palette of a chunk.
 */
public class Palette {
    private static boolean maskBedrock = false;
    int bitsPerBlock;
    int[] palette;

    private Palette(int bitsPerBlock, int[] palette) {
        this.bitsPerBlock = bitsPerBlock;
        this.palette = palette;

        if (bitsPerBlock > 8) {
            System.out.println("WARNING: palette type not supported");
        }
    }

    public static void setMaskBedrock(boolean maskBedrock) {
        Palette.maskBedrock = maskBedrock;
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

    public int stateFromId(int data) {
        if (bitsPerBlock > 8) {
            return data;
        }
        if (palette.length == data) {
            System.out.println("Index oob! : " + data + " // " + bitsPerBlock);
        }
        return palette[data];
    }

    public boolean isEmpty() {
        return palette.length == 0 || (palette.length == 1 && palette[0] == 0);
    }

    public List<SpecificTag> toNbt() {
        List<SpecificTag> tags = new ArrayList<>();
        GlobalPalette globalPalette = WorldManager.getGlobalPalette();
        for (int i : palette) {
            tags.add(globalPalette.getState(i).toNbt());
        }
        return tags;
    }
}
