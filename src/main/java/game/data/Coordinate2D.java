package game.data;

import java.util.Objects;

public class Coordinate2D {
    static int offsetX = 0;
    static int offsetZ = 0;
    int x;
    int z;

    public Coordinate2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Coordinate2D(double x, double z) {
        this.x = (int) x;
        this.z = (int) z;
    }

    public static void setOffset(int x, int z) {
        offsetX = x;
        offsetZ = z;
    }

    public Coordinate2D offsetGlobal() {
        this.x += offsetX;
        this.z += offsetZ;
        return this;
    }

    public Coordinate2D offsetChunk() {
        this.x += offsetX / 16;
        this.z += offsetZ / 16;
        return this;
    }

    public boolean isInRange(Coordinate2D other, int distance) {
        return Math.abs(this.x - other.x) + Math.abs(this.z - other.z) <= distance;
    }

    public Coordinate2D chunkToRegion() {
        return new Coordinate2D(x >> 5, z >> 5);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Coordinate2D that = (Coordinate2D) o;
        return x == that.x &&
            z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + z + ")";
    }

    public Coordinate2D toRegionLocal() {
        return new Coordinate2D(toLocal(x), toLocal(z));
    }

    private int toLocal(int pos) {
        pos = pos % 32;
        if (pos < 0) {
            return pos + 32;
        }
        return pos;
    }
}
