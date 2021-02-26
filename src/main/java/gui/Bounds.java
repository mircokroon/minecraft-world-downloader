package gui;

import game.data.coordinates.Coordinate2D;

public class Bounds {
    private int minX, maxX, minZ, maxZ;

    public Bounds() {
        reset();
    }

    public Bounds(Coordinate2D center, int renderDistanceX, int renderDistanceZ) {
        this.minX = center.getX() - renderDistanceX;
        this.maxX = center.getX() + renderDistanceX;

        this.minZ = center.getZ() - renderDistanceZ;
        this.maxZ = center.getZ() + renderDistanceZ;
    }

    public void reset() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
    }

    public void update(Coordinate2D coord) {
        updateX(coord.getX());
        updateZ(coord.getZ());
    }

    private void updateX(int x) {
        if (x < minX) { minX = x; }
        if (x > maxX) { maxX = x; }
    }

    private void updateZ(int z) {
        if (z < minZ) { minZ = z; }
        if (z > maxZ) { maxZ = z; }
    }

    public int getWidth() {
        return Math.abs(getMaxX() - getMinX()) + 1;
    }

    public int getHeight() {
        return Math.abs(getMaxZ() - getMinZ()) + 1;
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

    public Coordinate2D center(int gridSize, double width, double height) {
        int trueMaxX = (int) (minX + (width / gridSize));
        int trueMaxZ = (int) (minZ + (height / gridSize));
        return new Coordinate2D((trueMaxX + minX) / 2, (trueMaxZ + minZ) / 2);
    }
}
