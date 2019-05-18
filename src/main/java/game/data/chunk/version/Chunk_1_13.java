package game.data.chunk.version;

import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.Tag;

import game.Game;
import game.data.Dimension;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.Palette;
import packets.DataTypeProvider;

public class Chunk_1_13 extends Chunk {

    private int[] biomes;

    public Chunk_1_13(int x, int z) {
        super(x, z);
        biomes = new int[256];
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette, int bitsPerBlock) {
        return new ChunkSection_1_13(y, palette);
    }


    private void setBiome(int x, int z, int biomeId) {
        biomes[z * 16 + x] = biomeId;
    }

    @Override
    protected void readBiomes(DataTypeProvider dataProvider) {
        for (int z = 0; z < SECTION_WIDTH; z++) {
            for (int x = 0; x < SECTION_WIDTH; x++) {
                setBiome(x, z, dataProvider.readInt());
            }
        }
    }

    protected Tag getBiomes() {
        return new IntArrayTag("Biomes", biomes);
    }
}
