package game.data;

public class Coordinate3D {
    private static int offsetX;
    private static int offsetZ;

    int x;
    int y;
    int z;

    public Coordinate3D(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
    }

    public Coordinate3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // set the offset and round it to chunks
    public static void setOffset(int x, int z) {
        offsetX = x >> 4 << 4;
        offsetZ = x >> 4 << 4;
    }

    public Coordinate3D offset() {
        this.x += offsetX;
        this.z += offsetZ;
        return this;
    }


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int localX() {
        return toLocal(x);
    }

    private int toLocal(int pos) {
        pos = pos % 16;
        if (pos < 0) {
            return pos + 16;
        }
        return pos;
    }

    public int localY() {
        return toLocal(y);
    }

    public int localZ() {
        return toLocal(z);
    }

    public Coordinate2D chunkPos() {
        return new Coordinate2D(chunkPosX(), chunkPosZ());
    }

    public int chunkPosX() {
        return getChunk(x);
    }

    public int chunkPosZ() {
        return getChunk(z);
    }

    private int getChunk(int pos) {
        return pos >> 4;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
