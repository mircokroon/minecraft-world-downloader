package gui.markers;

import game.data.coordinates.Coordinate2D;
import gui.Bounds;
import java.util.ArrayList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import util.PrintUtils;

public class MapMarkerHandler {
    Coordinate2D measureSource;

    public MapMarkerHandler() {
    }

    public void setMarker(Coordinate2D pos) {
        measureSource = pos;
    }

    public int getDistance(Coordinate2D cursorPos) {
        if (measureSource == null) {
            return -1;
        }
        return cursorPos.distance(measureSource);
    }

    public void draw(Bounds bounds, double blocksPerPixel, GraphicsContext graphics, Coordinate2D cursorPos) {
        if (measureSource == null) {
            return;
        }

        int markerSize = 8;
        double markerX = ((measureSource.getX() - bounds.getMinX() - 0.5) / blocksPerPixel);
        double markerZ = ((measureSource.getZ() - bounds.getMinZ() - 0.5) / blocksPerPixel);

        // line
        if (cursorPos != null) {
            double destX = (cursorPos.getX() - bounds.getMinX() - 0.5) / blocksPerPixel;
            double destZ = (cursorPos.getZ() - bounds.getMinZ() - 0.5) / blocksPerPixel;

            graphics.setStroke(Color.WHITE);
            graphics.strokeLine(markerX, markerZ, destX, destZ);
        }

        // marker
        graphics.setFillRule(FillRule.NON_ZERO);
        graphics.setFill(Color.WHITE);
        graphics.fillOval(markerX - markerSize / 2, markerZ - markerSize / 2, 8, 8);
        graphics.setStroke(Color.BLACK);
        graphics.strokeOval(markerX - markerSize / 2, markerZ - markerSize / 2, 8, 8);
    }
}
