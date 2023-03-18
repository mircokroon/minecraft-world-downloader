package game.data.chunk.version;

import config.Version;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.DirectPalette;
import game.data.chunk.palette.Palette;
import game.data.chunk.palette.PaletteBuilder;
import game.data.coordinates.Coordinate3D;
import game.data.dimension.Dimension;
import java.util.Optional;
import javafx.util.Pair;
import packets.builder.PacketBuilder;
import se.llbit.nbt.ByteArrayTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.Tag;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

/**
 * Chunk sections in 1.12 require parsing of the full block data as the level format does not include a palette
 * as the new versions do. As such this class is more lengthy than the new versions.
 */
public class ChunkSection_1_12 extends ChunkSection {
    protected int[][][] blockStates;

    public ChunkSection_1_12(byte y, Palette palette, Chunk chunk) {
        super(y, palette, chunk);
        this.blockStates = new int[16][16][16];
    }

    public ChunkSection_1_12(int sectionY, Tag nbt, Chunk chunk) {
        super(sectionY, chunk);
        this.blockStates = new int[16][16][16];
        this.palette = new DirectPalette();

        long[] blocks = encodeBlocks(nbt.get("Blocks").byteArray(), nbt.get("Data").byteArray(), this.palette);

        this.setBlocks(blocks);
        this.setBlockLight(nbt.get("BlockLight").byteArray());
        this.setSkyLight(nbt.get("SkyLight").byteArray());
    }

    @Override
    public void setBlocks(long[] blocks) {
        super.setBlocks(blocks);

        if (blocks.length == 0) { return; }

        for (int y = 0; y < Chunk.SECTION_HEIGHT; y++) {
            for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {
                for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
                    int data = getPaletteIndex(x, y, z);
                    this.blockStates[x][y][z] = palette.stateFromId(data);
                }
            }
        }
    }

    @Override
    protected void addNbtTags(CompoundTag map) {
        Pair<byte[], Optional<byte[]>> blocks = getBlockIds();

        map.add("Blocks", new ByteArrayTag(blocks.getKey()));
        map.add("Data", new ByteArrayTag(getBlockStates()));

        if (blocks.getValue().isPresent()) {
            map.add("Add", new ByteArrayTag(blocks.getValue().get()));
        }
    }

    private Pair<byte[], Optional<byte[]>> getBlockIds() {
        byte[] blockData = new byte[4096];
        byte[] additional = new byte[2048];
        boolean hasAdditionalData = false;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    int blockState = blockStates[x][y][z] >>> 4;
                    int index = getBlockIndex(x, y, z);
                    blockData[index] = (byte) (blockState);

                    // excess data is packed into half a byte per block, for Forge
                    int excess = blockState >>> 8;
                    if (excess > 0) {
                        hasAdditionalData = true;

                        // if index is odd, shift by 4, otherwise leave unchanged
                        additional[index / 2] |= (byte) (excess << ((index % 2) * 4));;
                    }
                }
            }
        }

        return new Pair<>(blockData, hasAdditionalData ? Optional.of(additional) : Optional.empty());
    }

    private byte[] getBlockStates() {
        byte[] blockData = new byte[2048];

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    insertAtHalf(blockData, x, y, z, blockStates[x][y][z] & 0x0F);
                }
            }
        }
        return blockData;
    }

    private long[] encodeBlocks(byte[] blocks, byte[] data, Palette palette) {
        return encodeBlocks(index -> {
            int blockState = (blocks[index] & 0xFF) << 4;
            int b = data[index/2] & 0xFF;
            if (index % 2 != 0) {
                b = b >>> 4;
            }
            blockState |= b & 0x0F;
            blockState &= 0xFFF;

            return palette.stateFromId(blockState);
        }, blocks.length, palette.getBitsPerBlock());
    }

    private static long[] encodeBlocks(IntUnaryOperator getState, int size, int bitsPerBlock) {
        long[] res = new long[(int) (Math.ceil(Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH * Chunk.SECTION_HEIGHT * bitsPerBlock * 1.0) / Long.SIZE)];
        for (int i = 0; i < size; i++) {
            int longIndex = (i * bitsPerBlock) / Long.SIZE;
            int endLong = ((i + 1) * bitsPerBlock - 1) / Long.SIZE;
            int indexInLong = (i * bitsPerBlock) % Long.SIZE;

            long toInsert = getState.applyAsInt(i);

            res[longIndex] |= (toInsert << indexInLong);

            if (longIndex != endLong) {
                res[endLong] = (toInsert >>> (64 - indexInLong));
            }
        }
        return res;
    }

    /**
     * Handle inserting of the four-bit values into the given array at the given coordinates. In this case, a value
     * takes up 4 bits so inserting them is a bit more complicated.
     */
    private static void insertAtHalf(byte[] arr, int x, int y, int z, int val) {
        int pos = getBlockIndex(x, y, z);
        boolean isUpperHalf = pos % 2 == 0;
        if (!isUpperHalf) {
            arr[pos / 2] |= (val << 4) & 0xF0;
        } else {
            arr[pos / 2] |= val & 0x0F;
        }
    }

    @Override
    public void write(PacketBuilder packet) {
        PaletteBuilder paletteBuilder = new PaletteBuilder(blockStates);
        Palette networkPalette = paletteBuilder.build();
        int[] indices = paletteBuilder.getIndices();
        long[] blocks = encodeBlocks(i -> indices[i] & 0xFFF, indices.length, networkPalette.getBitsPerBlock());

        networkPalette.write(packet);
        packet.writeVarInt(blocks.length);

        packet.writeLongArray(blocks);
        packet.writeByteArray(this.blockLight);

        if (WorldManager.getInstance().getDimension() != Dimension.NETHER) {
            packet.writeByteArray(this.skyLight);
        }
    }

    @Override
    public int getNumericBlockStateAt(int x, int y, int z) {
        return blockStates[x][y][z];
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.deepHashCode(blockStates);
        return result;
    }

    @Override
    public void setBlockAt(Coordinate3D coords, int blockStateId) {
        super.setBlockAt(coords, blockStateId);
        this.blockStates[coords.getX()][coords.getY()][coords.getZ()] = blockStateId;
    }

    @Override
    public String toString() {
        return "ChunkSection_1_12{" +
                "blockStates=" + (Arrays.toString(blockStates[0][0])) +
                '}';
    }
}
