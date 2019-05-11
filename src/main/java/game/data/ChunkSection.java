package game.data;

public class ChunkSection {
    BlockState[][][] blocks;
    int[][][] blockLight;
    int[][][] skyLight;

    public ChunkSection() {
        this.blocks = new BlockState[16][16][16];
        this.blockLight = new int[16][16][16];
        this.skyLight = new int[16][16][16];
    }

    public void setState(int x, int y, int z, BlockState state) {
        this.blocks[x][y][z] = state;
    }

    public void setBlockLight(int x, int y, int z, int lightValue) {
        this.blockLight[x][y][z] = lightValue;
    }

    public void setSkyLight(int x, int y, int z, int lightValue) {
        this.skyLight[x][y][z] = lightValue;
    }

    public BlockInformation getBlockInformation(Coordinate3D coordinate) {
        int x = coordinate.localX();
        int y = coordinate.localY();
        int z = coordinate.localZ();

        return new BlockInformation(blocks[x][y][z], skyLight[x][y][z], blockLight[x][y][z]);
    }
}
