package game.data.chunk.version;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.Tag;

import game.Game;
import game.data.Dimension;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import packets.DataTypeProvider;

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

    protected Tag getBiomes() {
        return new ByteArrayTag("Biomes", biomes);
    }
}
