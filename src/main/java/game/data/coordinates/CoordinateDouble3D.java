package game.data.coordinates;

public class CoordinateDouble3D extends CoordinateDouble2D {
    double y;

    public CoordinateDouble3D(double x, double y, double z) {
        super(x, z);
        this.y = y;
    }

    public Coordinate3D discretize() {
        return new Coordinate3D(x, y, z);
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "CoordinateDouble3D{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public void increment(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }

    public void setTo(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
