package game.data.chunk.palette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import config.Config;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import game.data.chunk.version.encoder.BlockLocationEncoder_1_16;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PaletteTransformerTest {

    @BeforeAll
    public static void setup() {
        Config cfg = mock(Config.class);
        Config.setInstance(cfg);
    }

    @Test
    public void checkConversion() {
        // init data
        DirectPalette oldPalette = new DirectPalette(15);
        BlockLocationEncoder ble = new BlockLocationEncoder_1_16();
        long[] blocks = new long[1024];

        // write unique block into every position
        int i = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    ble.setTo(x, y, z, oldPalette.getBitsPerBlock());
                    ble.write(blocks, i++);
                }
            }
        }

        // transform
        PaletteTransformer transformer = new PaletteTransformer(ble, oldPalette);
        long[] newBlocks = transformer.transform(blocks);
        Palette newPalette = transformer.getNewPalette();

        // verify all blocks are the same as before
        i = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    ble.setTo(x, y, z, newPalette.getBitsPerBlock());

                    assertThat(i++)
                        .as(x + ", " + y  + ", " + z )
                        .isEqualTo(ble.fetch(newBlocks));
                }
            }
        }
    }
}
