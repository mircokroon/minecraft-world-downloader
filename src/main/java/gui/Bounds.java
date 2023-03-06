package gui;

import game.data.coordinates.Coordinate2D;

public class Bounds {
    private int minX, maxX, minZ, maxZ;

    public Bounds(Coordinate2D center, int renderDistanceX, int renderDistanceZ) {
        int margin = 4;

        int radiusX = (renderDistanceX / 2) + margin;
        int radiusZ = (renderDistanceZ / 2) + margin;

        this.minX = center.getX() - radiusX;
        this.maxX = center.getX() + radiusX;

        this.minZ = center.getZ() - radiusZ;
        this.maxZ = center.getZ() + radiusZ;
    }

    public void reset() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    @Override
    public String toString() {
        return "Bounds{" +
            "minX=" + minX +
            ", maxX=" + maxX +
            ", minZ=" + minZ +
            ", maxZ=" + maxZ +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bounds bounds = (Bounds) o;

        if (minX != bounds.minX) return false;
        if (maxX != bounds.maxX) return false;
        if (minZ != bounds.minZ) return false;
        return maxZ == bounds.maxZ;
    }

    @Override
    public int hashCode() {
        int result = minX;
        result = 31 * result + maxX;
        result = 31 * result + minZ;
        result = 31 * result + maxZ;
        return result;
    }

    public boolean overlaps(Coordinate2D region) {
        int x1 = region.getX() << 9;
        int x2 = x1 + 512;

        int z1 = region.getZ() << 9;
        int z2 = z1 + 512;

        return maxX > x1 && minX < x2 && minZ < z2 && maxZ > z1;
    }
}
