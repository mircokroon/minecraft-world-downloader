package game.data;

import java.util.Objects;

public class Coordinate2D {
    int x;
    int z;

    public Coordinate2D(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Coordinate2D toRegion() {
        return new Coordinate2D(x / 32, z / 32);
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
}
