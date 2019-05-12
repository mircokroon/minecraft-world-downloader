package game.data;

public class Coordinate3D {
    int x;
    int y;
    int z;

    public Coordinate3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate3D(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
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

    public int localY() {
        return toLocal(y);
    }

    public int localZ() {
        return toLocal(z);
    }

    public int chunkPosX() {
        return getChunk(x);
    }

    public int chunkPosZ() {
        return getChunk(z);
    }

    public Coordinate2D chunkPos() {
        return new Coordinate2D(chunkPosX(), chunkPosZ());
    }

    private int getChunk(int pos) {
        return (int) Math.floor(pos / 16);
    }

    private int toLocal(int pos) {
        pos = pos % 16;
        if (pos < 0) {
            return pos + 16;
        }
        return pos;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
