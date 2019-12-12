package game.data.chunk.version;

import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

/**
 * Chunk format for 1.13+. Now includes a status tag and the biomes are integers.
 */
public class Chunk_1_13 extends Chunk {

    private int[] biomes;

    public Chunk_1_13(int x, int z) {
        super(x, z);
        biomes = new int[256];
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
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

    @Override
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("Status", new StringTag("postprocessed"));

        // empty maps
        CompoundTag carvingMasks = new CompoundTag();
        carvingMasks.add("AIR", new ByteArrayTag(new byte[0]));
        carvingMasks.add("LIQUID", new ByteArrayTag(new byte[0]));
        map.add("CarvingMasks", carvingMasks);

        CompoundTag structures = new CompoundTag();
        structures.add("References", new CompoundTag());
        structures.add("Starts", new CompoundTag());
        map.add("Structures", structures);

        super.addLevelNbtTags(map);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_13(sectionY, section);
    }


    protected SpecificTag getBiomes() {
        return new IntArrayTag(biomes);
    }
}
