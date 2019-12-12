package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;

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
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_12(y, palette);
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

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("TerrainPopulated", new ByteTag((byte) 1));
        map.add("LightPopulated", new ByteTag((byte) 1));

        super.addLevelNbtTags(map);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_12(sectionY, section);
    }

    protected SpecificTag getBiomes() {
        return new ByteArrayTag(biomes);
    }


}
