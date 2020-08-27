package game.data;

public class CoordinateDimension2D extends Coordinate2D {
    Dimension dimension;

    public CoordinateDimension2D(Coordinate2D coordinate2D, Dimension dimension) {
        super(coordinate2D.x, coordinate2D.z);
        this.dimension = dimension;
    }

    public CoordinateDimension2D(int x, int z, Dimension dimension) {
        super(x, z);
        this.dimension = dimension;
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

        CoordinateDimension2D that = (CoordinateDimension2D) o;

        return dimension == that.dimension;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dimension != null ? dimension.hashCode() : 0);
        return result;
    }

    public Dimension getDimension() {
        return dimension;
    }
}
