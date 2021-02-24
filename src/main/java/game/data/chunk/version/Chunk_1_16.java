package game.data.chunk.version;

import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Support for chunks of version 1.16.2+. 1.16.0 and 1.16.1 are not supported.
 */
public class Chunk_1_16 extends Chunk_1_15 {
    public static final int DATA_VERSION = 2578;

    public Chunk_1_16(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }


    // 1.16.2 changes biomes from int[1024] to varint[given length]
    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        int biomesLength = provider.readVarInt();
        setBiomes(provider.readVarIntArray(biomesLength));
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_16(y, palette);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_16(sectionY, section);
    }

    @Override
    protected void writeBiomes(PacketBuilder packet) {
        int[] biomes = getBiomes();
        packet.writeVarInt(biomes.length);
        packet.writeVarIntArray(biomes);
    }

    @Override
    public void updateBlocks(Coordinate3D pos, DataTypeProvider provider) {
        provider.readBoolean();

        int count = provider.readVarInt();
        Collection<Coordinate3D> toUpdate = new ArrayList<>();
        while (count-- > 0) {
            long blockChange = provider.readVarLong();
            int blockId = (int) blockChange >>> 12;

            int x = (int) (blockChange >> 8) & 0x0F;
            int z = (int) (blockChange >> 4) & 0x0F;
            int y = (int) (blockChange     ) & 0x0F;

            // since updateBlock expects the height to be [0-256], we add in the section coordinates.
            Coordinate3D blockPos = new Coordinate3D(x, pos.getY() * 16 + y, z);
            toUpdate.add(blockPos);

            updateBlock(blockPos, blockId, true);
        }
        this.getChunkImageFactory().recomputeHeights(toUpdate);
    }
}
