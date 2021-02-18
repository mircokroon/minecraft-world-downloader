package game.data.coordinates;

import config.Config;
import game.data.dimension.Dimension;

public class Coordinate2D {
    private static final int CHUNK_SHIFT = 4;
    private static final int REGION_SHIFT = 5;
    private static final int REGION_TOTAL_SHIFT = CHUNK_SHIFT + REGION_SHIFT;
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

    public Coordinate2D offsetChunk() {
        return new Coordinate2D(
                x - (Config.getCenterX() >> CHUNK_SHIFT),
                z - (Config.getCenterZ() >> CHUNK_SHIFT)
        );
    }

    public Coordinate2D offsetRegion() {
        return new Coordinate2D(
                x - (Config.getCenterX() >> REGION_TOTAL_SHIFT),
                z - (Config.getCenterZ() >> REGION_TOTAL_SHIFT)
        );
    }

    public Coordinate2D offsetRegionToActual() {
        return new Coordinate2D(
                x + (Config.getCenterX() >> REGION_TOTAL_SHIFT),
                z + (Config.getCenterZ() >> REGION_TOTAL_SHIFT)
        );
    }

    public boolean isInRange(Coordinate2D other, int distanceX, int distanceZ) {
        return Math.abs(this.x - other.x) <= distanceX && Math.abs(this.z - other.z) <= distanceZ;
    }

    public boolean isInRange(Coordinate2D other, int distance) {
        return isInRange(other, distance, distance);
    }

    public Coordinate2D subtract(Coordinate2D other) {
        return new Coordinate2D(this.x - other.x, this.z - other.z);
    }

    public Coordinate2D add(int x, int z) {
        return new Coordinate2D(this.x + x, this.z + z);
    }

    public Coordinate2D add(Coordinate2D other) {
        return add(other.x, other.z);
    }

    public Coordinate2D globalToChunk() {
        return new Coordinate2D(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT);
    }
    public Coordinate2D chunkToRegion() {
        return new Coordinate2D(x >> REGION_SHIFT, z >> REGION_SHIFT);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return 31 * z + x;
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

    public int blockDistance(Coordinate2D globalToChunk) {
        return Math.max(Math.abs(this.x - globalToChunk.x), Math.abs(this.z - globalToChunk.z));
    }

    public Coordinate2D divide(int size) {
        if (size == 0) {
            return this;
        }
        return new Coordinate2D(this.x / size, this.z / size);
    }
}
