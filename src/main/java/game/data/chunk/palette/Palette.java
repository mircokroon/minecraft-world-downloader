package game.data.chunk.palette;

import game.data.chunk.ChunkSection;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.SpecificTag;

import java.util.*;

/**
 * Class to hold a palette of a chunk.
 */
public class Palette {
    protected int bitsPerBlock;
    private int[] palette;

    protected Palette() { }

    private Palette(int bitsPerBlock, int[] palette) {
        this.bitsPerBlock = bitsPerBlock;
        this.palette = palette;

        synchronizeBitsPerBlock();
    }

    Palette(int[] arr) {
        this.palette = arr;
        this.bitsPerBlock = computeBitsPerBlock(arr.length - 1);
    }

    public static Palette empty() {
        return new Palette(4, new int[1]);
    }


    /**
     * Some non-vanilla servers will use more bits per block than needed, which will cause
     * issues when reading in the chunk later. To fix this, we increase the size of the
     * palette array by by adding unused block states.
     */
    private void synchronizeBitsPerBlock() {
        if (this.bitsPerBlock > 16) {
            throw new IllegalArgumentException("Bits per block may not be more than 16. Given: " + this.bitsPerBlock);
        }

        while (this.bitsPerBlock > computeBitsPerBlock(palette.length - 1)) {
            int[] newPalette = new int[palette.length + 1];
            System.arraycopy(palette, 0, newPalette, 0, palette.length);
            this.palette = newPalette;
        }
    }

    public Palette(int dataVersion, ListTag nbt) {
        this.bitsPerBlock = computeBitsPerBlock(nbt.size() - 1);
        this.palette = new int[nbt.size()];

        GlobalPalette global = GlobalPaletteProvider.getGlobalPalette(dataVersion);
        for (int i = 0; i < nbt.size(); i++) {
            BlockState bs = global.getState(nbt.get(i).asCompound());

            // if a block is unknown, just leave it at 0
            if (bs != null) {
                this.palette[i] = bs.getNumericId();
            }
        }
    }

    private int computeBitsPerBlock(int maxIndex) {
        int bitsNeeded = Integer.SIZE - Integer.numberOfLeadingZeros(maxIndex);
        return Math.max(4, bitsNeeded);
    }

    /**
     * Read the palette from the network stream.
     * @param bitsPerBlock the number of bits per block that is used, indicates the palette type
     * @param dataTypeProvider network stream reader
     */
    public static Palette readPalette(int bitsPerBlock, DataTypeProvider dataTypeProvider) {
        if (bitsPerBlock > 8) {
            return new DirectPalette(bitsPerBlock);
        }
        int size = dataTypeProvider.readVarInt();

        int[] palette = dataTypeProvider.readVarIntArray(size);

        return new Palette(bitsPerBlock, palette);
    }

    /**
     * Get the block state from the palette index.
     */
    public int stateFromId(int index) {
        if (bitsPerBlock > 8) {
            return index;
        }
        if (palette.length == 0) {
            return 0;
        }
        if (index >= palette.length) {
            return 0;
        }

        return palette[index];
    }

    public boolean isEmpty() {
        return palette.length == 0 || (palette.length == 1 && palette[0] == 0);
    }

    /**
     * Create an NBT version of this palette using the global palette.
     */
    public List<SpecificTag> toNbt() {
        List<SpecificTag> tags = new ArrayList<>();
        GlobalPalette globalPalette = GlobalPaletteProvider.getGlobalPalette();

        if (globalPalette == null) {
            throw new UnsupportedOperationException("Cannot create palette NBT without a global palette.");
        }

        for (int i : palette) {
            BlockState state = globalPalette.getState(i);
            if (state == null) { state = globalPalette.getDefaultState(); }

            tags.add(state.toNbt());

        }
        return tags;
    }

    public int getBitsPerBlock() {
        return bitsPerBlock;
    }

    public void write(PacketBuilder packet) {
        packet.writeByte((byte) bitsPerBlock);
        packet.writeVarInt(palette.length);
        packet.writeVarIntArray(palette);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Palette palette1 = (Palette) o;

        if (bitsPerBlock != palette1.bitsPerBlock) return false;
        return Arrays.equals(palette, palette1.palette);
    }

    @Override
    public int hashCode() {
        int result = bitsPerBlock;
        result = 31 * result + Arrays.hashCode(palette);
        return result;
    }

    @Override
    public String toString() {
        return "Palette{" +
                "bitsPerBlock=" + bitsPerBlock +
                ", palette(" + palette.length + ")=" + Arrays.toString(palette) +
                '}';
    }


    public int getIndexFor(ChunkSection section, int blockStateId) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == blockStateId) {
                return i;
            }
        }

        int[] newPalette = new int[palette.length + 1];
        System.arraycopy(palette, 0, newPalette, 0, palette.length);
        newPalette[palette.length] = blockStateId;
        this.palette = newPalette;

        int newBitsPerBlock = computeBitsPerBlock(palette.length - 1);
        if (bitsPerBlock != newBitsPerBlock) {
            section.resizeBlocks(newBitsPerBlock);
            this.bitsPerBlock = newBitsPerBlock;
        }

        return palette.length - 1;
    }


    public int size() {
        return palette.length;
    }
}
