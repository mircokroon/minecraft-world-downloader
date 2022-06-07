package game.data.chunk.palette;

import game.data.chunk.version.ChunkSection_1_16;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import java.util.Arrays;

/**
 * Transforms a direct palette to a real palette.
 */
public class PaletteTransformer {
    BlockLocationEncoder locationEncoder;
    Palette oldPalette;
    Palette newPalette;

    public PaletteTransformer(BlockLocationEncoder locationEncoder, DirectPalette oldPalette) {
        this.locationEncoder = locationEncoder;
        this.oldPalette = oldPalette;
        this.newPalette = new Palette(new int[] { });

        if (oldPalette.type == PaletteType.BIOMES) {
            newPalette.biomePalette();
        }
    }

    public long[] transform(long[] data) {
        if (data.length == 0) {
            return data;
        }

        // first add every block in the chunk to the new palette
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    locationEncoder.setTo(x, y, z, oldPalette.getBitsPerBlock());
                    newPalette.getIndexFor(null, locationEncoder.fetch(data));
                }
            }
        }

        newPalette.recomputeBitsPerBlock();

        // copy all blocks to the new palette
        long[] newData = new long[ChunkSection_1_16.longsRequired(newPalette.getBitsPerBlock())];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    locationEncoder.setTo(x, y, z, oldPalette.getBitsPerBlock());
                    int index = newPalette.getIndexFor(null, locationEncoder.fetch(data));

                    locationEncoder.setTo(x, y, z, newPalette.getBitsPerBlock());
                    locationEncoder.write(newData, index);
                }
            }
        }
        return newData;
    }

    public Palette getNewPalette() {
        return newPalette;
    }
}
