package gui;

import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDouble2D;

public class Bounds {
    private double minX, maxX, minZ, maxZ;

    public void reset() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxZ() {
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

    public boolean overlaps(Coordinate2D region) {
        int x1 = region.getX() << 9;
        int x2 = x1 + 512;

        int z1 = region.getZ() << 9;
        int z2 = z1 + 512;

        return maxX > x1 && minX < x2 && minZ < z2 && maxZ > z1;
    }

    public void set(CoordinateDouble2D center, double renderDistanceX, double renderDistanceZ) {
        double radiusX = (renderDistanceX / 2);
        double radiusZ = (renderDistanceZ / 2);

        this.minX = center.getX() - radiusX;
        this.maxX = center.getX() + radiusX;

        this.minZ = center.getZ() - radiusZ;
        this.maxZ = center.getZ() + radiusZ;
    }
}
