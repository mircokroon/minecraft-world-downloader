package game.data.coordinates;

import game.data.dimension.Dimension;

/**
 * 2D coordinates with a dimension component.
 */
public class CoordinateDim2D extends Coordinate2D {
    Dimension dimension;

    public CoordinateDim2D(Coordinate2D coordinate2D, Dimension dimension) {
        super(coordinate2D.x, coordinate2D.z);
        this.dimension = dimension;
    }

    public CoordinateDim2D(int x, int z, Dimension dimension) {
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

        CoordinateDim2D that = (CoordinateDim2D) o;

        return dimension == that.dimension;
    }

    public CoordinateDim2D addWithDimension(int x, int z) {
        return new CoordinateDim2D(this.x + x, this.z + z, this.dimension);
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

    public CoordinateDim2D chunkToDimRegion() {
        return new CoordinateDim2D(this.chunkToRegion(), this.dimension);
    }

    public CoordinateDim2D copy() {
        return new CoordinateDim2D(x, z, dimension);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + z + ", " + dimension + ")";
    }
}
