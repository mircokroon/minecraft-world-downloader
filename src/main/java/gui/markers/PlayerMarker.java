package gui.markers;

public class PlayerMarker extends MapMarker {
    static double[] xPoints = { 0, 8.5, 0, -8.5 };
    static double[] yPoints = { 12, -8, -4, -8 };

    public PlayerMarker() {
        super(xPoints.length);
    }

    @Override
    double[] getShapePointsX() {
        return xPoints;
    }

    @Override
    double[] getShapePointsY() {
        return yPoints;
    }
}
