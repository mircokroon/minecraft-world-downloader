package game.data.chunk.version;

import game.data.chunk.palette.Palette;
import game.data.chunk.palette.PaletteType;
import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;

public class Chunk_1_20_2 extends Chunk_1_20 {


    public Chunk_1_20_2(CoordinateDim2D location, int version) {
        super(location, version);
    }

    /**
     * Read a chunk column for 1.20.2
     */
    public void readChunkColumn(DataTypeProvider dataProvider) {
        // Loop through section Y values, starting from the lowest section that has blocks inside it.
        for (int sectionY = getMinBlockSection(); sectionY <= getMaxBlockSection() && dataProvider.hasNext(); sectionY++) {
            ChunkSection_1_18 section = (ChunkSection_1_18) getChunkSection(sectionY);

            dataProvider.readShort();
            Palette blockPalette = Palette.readPalette(dataProvider, PaletteType.BLOCKS);

            if (section == null) {
                section = (ChunkSection_1_18) createNewChunkSection((byte) (sectionY & 0xFF), blockPalette);
            } else {
                section.setBlockPalette(blockPalette);
            }

            section.setBlocks(dataProvider.readLongArray(dataProvider.readVarInt()));

            Palette biomePalette = Palette.readPalette(dataProvider, PaletteType.BIOMES);
            section.setBiomePalette(biomePalette);

            // check how many longs we expect, if there's more discard the rest
            int longsExpectedBiomes = ChunkSection_1_18.longsRequiredBiomes(biomePalette.getBitsPerBlock());
            section.setBiomes(dataProvider.readLongArray(dataProvider.readVarInt()));

            // May replace an existing section or a null one
            setChunkSection(sectionY, section);

            // servers don't (always?) include containers in the list of block_entities. We need to know that these block
            // entities exist, otherwise we'll end up not writing block entity data for them
            if (containsBlockEntities(blockPalette)) {
                findBlockEntities(section, sectionY);
            }
        }
    }
}
