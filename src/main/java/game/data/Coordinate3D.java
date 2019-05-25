package game.data;

public class Coordinate3D extends Coordinate2D {
    private int y;

    public Coordinate3D(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
    }

    public Coordinate3D(int x, int y, int z) {
        super(x, z);
        this.y = y;
    }

    public void setTo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getY() {
        return y;
    }
    public Coordinate2D chunkPos() {
        return new Coordinate2D(chunkPosX(), chunkPosZ());
    }

    private int chunkPosX() {
        return x >> 4;
    }

    private int chunkPosZ() {
        return z >> 4;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
