package gui.markers;

import javafx.geometry.Point2D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javax.swing.tree.TreeNode;

public abstract class MapMarker {
    double[] xPoints;
    double[] yPoints;

    public void transform(double offsetX, double offsetZ, double rotation, double size) {
        double[] x = getShapePointsX();
        double[] y = getShapePointsY();

        xPoints = new double[x.length];
        yPoints = new double[y.length];

        Transform translate = new Translate(offsetX, offsetZ);
        Transform rotate = new Rotate(rotation);
        Transform scale = new Scale(size, size);

        for (int i = 0; i < x.length; i++) {
            Point2D p = new Point2D(x[i], y[i]);
            p = scale.transform(p);
            p = rotate.transform(p);
            p = translate.transform(p);

            xPoints[i] = p.getX();
            yPoints[i] = p.getY();
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
