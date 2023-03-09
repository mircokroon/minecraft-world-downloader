package game.data.coordinates;

public class CoordinateDouble2D {
    double x;
    double z;

    public CoordinateDouble2D(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public Coordinate2D discretize() {
        return new Coordinate2D(x, z);
    }

    public CoordinateDouble2D add(CoordinateDouble2D other) {
        return new CoordinateDouble2D(x + other.x, z + other.z);
    }
}
