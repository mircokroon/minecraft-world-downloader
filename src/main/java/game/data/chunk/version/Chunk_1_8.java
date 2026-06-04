package game.data.chunk.version;

import game.data.WorldManager;
import game.data.chunk.palette.DirectPalette;
import game.data.chunk.version.encoder.BlockLocationEncoder;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import packets.DataTypeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft 1.8 chunk format. This predates the per-section palette that was introduced in 1.9, so the
 * block data is sent as a raw array of little-endian shorts holding {@code (blockId << 4) | metadata},
 * with no palette. The column layout also differs from 1.9+: all section block arrays come first,
 * followed by all block-light arrays, then all sky-light arrays (overworld only), then the 256-byte
 * biome array for full chunks. The chunk-data packet carries no trailing block-entity array.
 *
 * On disk 1.8 uses the same anvil section format (Blocks/Data/BlockLight/SkyLight + byte biomes) as
 * 1.9-1.12, so we extend {@link Chunk_1_12} and only override the network parsing.
 */
public class Chunk_1_8 extends Chunk_1_12 {
    private static final int BLOCKS_PER_SECTION = 4096;
    private static final int LIGHT_BYTES = 2048;

    public Chunk_1_8(CoordinateDim2D location, int version) {
        super(location, version);
    }

    /**
     * Parse a 1.8 single Chunk Data packet: full flag, unsigned-short section bitmask, size, then the
     * column data.
     */
    @Override
    protected void parse(DataTypeProvider dataProvider) {
        raiseEvent("parse from packet");

        boolean full = dataProvider.readBoolean();
        if (!full) {
            markAsNew();
        }

        int bitmask = dataProvider.readShort() & 0xFFFF;
        int size = dataProvider.readVarInt();

        boolean skylight = WorldManager.getInstance().getDimension() != Dimension.NETHER;
        readColumn(full, bitmask, skylight, dataProvider.ofLength(size));

        afterParse();
    }

    /**
     * Read one chunk column sent as part of a Map Chunk Bulk packet. Such columns have no per-column
     * header (the bitmask comes from the packet's metadata block), are always full, and share a single
     * sky-light flag across the whole packet.
     */
    public void readBulkColumn(int bitmask, boolean skylight, DataTypeProvider dataProvider) {
        readColumn(true, bitmask, skylight, dataProvider);
        afterParse();
    }

    private void readColumn(boolean full, int bitmask, boolean skylight, DataTypeProvider provider) {
        List<ChunkSection_1_12> sections = new ArrayList<>();

        for (int y = 0; y <= getMaxLightSection(); y++) {
            if ((bitmask & (1 << y)) == 0) {
                continue;
            }

            int[][][] raw = new int[16][16][16];
            for (int i = 0; i < BLOCKS_PER_SECTION; i++) {
                int low = provider.readNext() & 0xFF;
                int high = provider.readNext() & 0xFF;
                int value = ((high << 8) | low) & 0xFFFF; // (blockId << 4) | metadata

                // 1.8 block index: i = (y << 8) | (z << 4) | x
                raw[i & 0xF][(i >> 8) & 0xF][(i >> 4) & 0xF] = value;
            }

            ChunkSection_1_12 section = buildSection((byte) y, raw);
            sections.add(section);
            setChunkSection(y, section);
        }

        for (ChunkSection_1_12 section : sections) {
            section.setBlockLight(provider.readByteArray(LIGHT_BYTES));
        }

        if (skylight) {
            for (ChunkSection_1_12 section : sections) {
                section.setSkyLight(provider.readByteArray(LIGHT_BYTES));
            }
        }

        if (full) {
            parse2DBiomeData(provider);
        }
    }

    /**
     * Build a section from raw {@code (blockId << 4) | metadata} values. We encode them with a direct
     * palette into the packed long array and hand it to {@link ChunkSection_1_12#setBlocks}, exactly as
     * the 1.12 disk loader does, which then derives the internal block-state grid.
     */
    private ChunkSection_1_12 buildSection(byte y, int[][][] raw) {
        DirectPalette palette = new DirectPalette();
        int bits = palette.getBitsPerBlock();

        long[] blocks = new long[64 * bits];
        BlockLocationEncoder encoder = new BlockLocationEncoder();
        for (int by = 0; by < 16; by++) {
            for (int bz = 0; bz < 16; bz++) {
                for (int bx = 0; bx < 16; bx++) {
                    encoder.setTo(bx, by, bz, bits).write(blocks, raw[bx][by][bz]);
                }
            }
        }

        ChunkSection_1_12 section = (ChunkSection_1_12) createNewChunkSection(y, palette);
        section.setBlocks(blocks);
        return section;
    }
}
