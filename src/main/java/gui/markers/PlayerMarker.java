package gui.markers;

public class PlayerMarker extends MapMarker {
    double[] xPoints = { 0, 8.5, 0, -8.5 };
    double[] yPoints = { 12, -8, -4, -8 };

    @Override
    double[] getShapePointsX() {
        return xPoints;
    }

    @Override
    double[] getShapePointsY() {
        return yPoints;
    }
}
