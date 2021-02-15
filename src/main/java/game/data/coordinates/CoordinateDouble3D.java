package game.data.coordinates;

public class CoordinateDouble3D {
    double x;
    double y;
    double z;

    public CoordinateDouble3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate3D discretize() {
        return new Coordinate3D(x, y, z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
