package game.data.chunk.palette;

public enum PaletteType {
    BLOCKS(4, 8),
    BIOMES(1, 3);

    private final int minBitsPerBlock, maxBitsPerBlock;

    PaletteType(int minBitsPerBlock, int maxBitsPerBlock) {
        this.minBitsPerBlock = minBitsPerBlock;
        this.maxBitsPerBlock = maxBitsPerBlock;
    }

    public int getMinBitsPerBlock() {
        return minBitsPerBlock;
    }

    public int getMaxBitsPerBlock() {
        return maxBitsPerBlock;
    }
}
