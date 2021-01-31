package game.data;

import game.data.dimension.Dimension;

import java.util.Objects;

public class Coordinate2D {
    private static int offsetX = 0;
    private static int offsetZ = 0;

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

    public void offset(int x, int z) {
        this.x += x;
        this.z += z;
    }

    public static void setOffset(int x, int z) {
        offsetX = x >> 4 << 4;
        offsetZ = z >> 4 << 4;
    }

    public void offsetGlobal() {
        this.x += offsetX;
        this.z += offsetZ;
    }

    public void offsetChunk() {
        this.x += offsetX >> 4;
        this.z += offsetZ >> 4;
    }

    public boolean isInRange(Coordinate2D other, int distanceX, int distanceZ) {
        return Math.abs(this.x - other.x) <= distanceX && Math.abs(this.z - other.z) <= distanceZ;
    }

    public boolean isInRange(Coordinate2D other, int distance) {
        return isInRange(other, distance, distance);
    }

    public Coordinate2D globalToChunk() {
        return new Coordinate2D(x >> 4, z >> 4);
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
    public int hashCode() {
        return Objects.hash(x, z);
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

    public CoordinateDim2D addDimension(Dimension dimension) {
        return new CoordinateDim2D(this, dimension);
    }
}
