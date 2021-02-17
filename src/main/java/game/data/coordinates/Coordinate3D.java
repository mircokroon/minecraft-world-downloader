package game.data.coordinates;

import config.Config;
import game.data.dimension.Dimension;

public class Coordinate3D extends Coordinate2D {
    private int y;

    public Coordinate3D(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
    }

    public Coordinate3D(int x, int y, int z) {
        super(x, z);
        this.y = y;
    }

    public Coordinate3D add(Coordinate2D change) {
        return new Coordinate3D(this.x + change.x, this.y, this.z + change.z);
    }

    public void setTo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coordinate3D offsetGlobal() {
        return new Coordinate3D(
                x - Config.getCenterX(),
                y,
                z - Config.getCenterZ()
        );
    }

    public int getY() {
        return y;
    }

    public Coordinate3D withinChunk() {
        return new Coordinate3D(withinChunk(x), y, withinChunk(z));
    }

    private int withinChunk(int val) {
        int newVal = val % 16;
        if (newVal < 0) { return newVal + 16; }
        return newVal;
    }

    public CoordinateDim3D addDimension3D(Dimension dimension) {
        return new CoordinateDim3D(this, dimension);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Coordinate3D that = (Coordinate3D) o;

        return y == that.y;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + y;
        return result;
    }

    public CoordinateDouble3D toDouble() {
        return new CoordinateDouble3D(x, y, z);
    }
}
