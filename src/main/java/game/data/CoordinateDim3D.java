package game.data;

import game.data.dimension.Dimension;

public class CoordinateDim3D extends Coordinate3D {
    private final Dimension dimension;

    public CoordinateDim3D(Coordinate3D pos, Dimension dimension) {
        super(pos.getX(), pos.getY(), pos.getZ());
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

        CoordinateDim3D that = (CoordinateDim3D) o;

        return dimension == that.dimension;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dimension != null ? dimension.hashCode() : 0);
        return result;
    }
}
