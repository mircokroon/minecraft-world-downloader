package game.data.container;

import game.data.Coordinate2D;

public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Coordinate2D toCoordinate() {
        switch (this) {
            case NORTH: return new Coordinate2D(0, -1);
            case EAST: return new Coordinate2D(1, 0);
            case SOUTH: return new Coordinate2D(0, 1);
            case WEST: return new Coordinate2D(-1, 0);
            default: return new Coordinate2D(0, 0);
        }
    }

    public Direction clockwise() {
        return getDirection(EAST, SOUTH, WEST, NORTH);
    }

    public Direction counterClockwise() {
        return getDirection(WEST, NORTH, EAST, SOUTH);
    }

    private Direction getDirection(Direction a, Direction b, Direction c, Direction d) {
        switch (this) {
            case NORTH: return a;
            case EAST: return b;
            case SOUTH: return c;
            case WEST: return d;
            default: return null;
        }
    }
}
