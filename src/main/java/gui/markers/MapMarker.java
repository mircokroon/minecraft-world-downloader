package gui.markers;

import com.sun.javafx.geom.transform.Translate2D;
import java.awt.geom.AffineTransform;
import javafx.geometry.Point2D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javax.swing.tree.TreeNode;

public abstract class MapMarker {
    int size;
    double[] yPoints, xPoints, inputPoints, outputPoints;

    AffineTransform transform;

    public MapMarker(int size) {
        this.size = size;
        this.xPoints = new double[size];
        this.yPoints = new double[size];
        this.inputPoints = new double[size * 2];
        this.outputPoints = new double[size * 2];

        double[] x = getShapePointsX();
        double[] y = getShapePointsY();
        for (int i = 0; i < size; i++) {
            this.inputPoints[i * 2] = x[i];
            this.inputPoints[i * 2 + 1] = y[i];
        }

        transform = new AffineTransform();
    }

    public void transform(double offsetX, double offsetZ, double rotation, double scale) {
        transform.setToIdentity();
        transform.translate(offsetX, offsetZ);
        transform.rotate(rotation * (Math.PI / 180));
        transform.scale(scale, scale);

        transform.transform(inputPoints, 0, outputPoints, 0, size);

        for (int i = 0; i < size; i++) {
            xPoints[i] = this.outputPoints[i * 2];
            yPoints[i] = this.outputPoints[i * 2 + 1];
        }
    }

    public int count() {
        return xPoints.length;
    }

    public double[] getPointsX() {
        return xPoints;
    }

    public double[] getPointsY() {
        return yPoints;
    }

    abstract double[] getShapePointsX();
    abstract double[] getShapePointsY();
}
