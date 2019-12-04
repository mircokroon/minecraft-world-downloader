package game.data.chunk.version;

import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import game.data.chunk.palette.BlockState;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.SpecificTag;

import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * Chunks in the 1.12(.2) format. Biomes were a byte array in this version.
 */
public class Chunk_1_12 extends Chunk {

    private byte[] biomes;

    public Chunk_1_12(int x, int z) {
        super(x, z);
        biomes = new byte[256];
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette, int bitsPerBlock) {
        return new ChunkSection_1_12(y, palette, bitsPerBlock);
    }

    private void setBiome(int x, int z, byte biomeId) {
        biomes[z * 16 + x] = biomeId;
    }

    @Override
    protected void readBiomes(DataTypeProvider dataProvider) {
        for (int z = 0; z < SECTION_WIDTH; z++) {
            for (int x = 0; x < SECTION_WIDTH; x++) {
                setBiome(x, z, dataProvider.readNext());
            }
        }
    }

    protected SpecificTag getBiomes() {
        return new ByteArrayTag(biomes);
    }

    @Override
    public Image getImage() {
        BufferedImage i = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);

        int newChunkMarker = 210 << 16;

        int color;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                color = 0;
                BlockState blockState = topmostBlockStateAt(x, z);

                if (blockState != null) {
                    color = blockState.getColor();
                }

                i.setRGB(x, z, color);

                // mark new chunks in a red-ish outline
                if (isNewChunk() && ((x == 0 || x == 15) || (z == 0 || z == 15))) {
                    i.setRGB(x, z, i.getRGB(x, z) ^ newChunkMarker);
                }
            }
        }

        return i;
    }
}
