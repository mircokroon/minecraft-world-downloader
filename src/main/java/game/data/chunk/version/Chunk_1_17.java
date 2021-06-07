package game.data.chunk.version;

import config.Version;
import game.data.WorldManager;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

import java.util.BitSet;

public class Chunk_1_17 extends Chunk_1_16 {
    public static final Version VERSION = Version.V1_17;

    @Override
    public int getDataVersion() { return VERSION.dataVersion; }

    static int minSectionY = 0;
    static int minBlockSectionY = -1;
    static int maxBlockSectionY = 15;
    static int fullHeight;

    public Chunk_1_17(CoordinateDim2D location) {
        super(location);
    }

    public static void setWorldHeight(int min_y, int height) {
        fullHeight = height;
        minBlockSectionY = min_y >> 4;
        minSectionY = minBlockSectionY - 1;
        maxBlockSectionY = minBlockSectionY + (height >> 4) - 1;
    }


    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_17(y, palette);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_17(sectionY, section);
    }

    /**
     * Parse the chunk data.
     *
     * @param dataProvider network input
     */
    @Override
    protected void parse(DataTypeProvider dataProvider) {
        raiseEvent("parse from packet");

        BitSet mask = dataProvider.readBitSet();

        parseHeightMaps(dataProvider);

        int biomeSize = dataProvider.readVarInt();
        setBiomes(dataProvider.readVarIntArray(biomeSize));

        int size = dataProvider.readVarInt();
        readChunkColumn(true, mask, dataProvider.ofLength(size));

        parseTileEntities(dataProvider);
        afterParse();
    }

    @Override
    protected int getMinSection() {
        return minSectionY;
    }
    @Override
    protected int getMinBlockSection() {
        return minBlockSectionY;
    }
    @Override
    protected int getMaxSection() {
        return maxBlockSectionY;
    }

}
