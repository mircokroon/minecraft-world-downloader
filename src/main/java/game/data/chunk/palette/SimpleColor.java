package game.data.chunk.palette;

import javafx.scene.paint.Color;

/**
 * Class to handle colours of blocks when building the overview image.
 */
public class SimpleColor {
    public static final SimpleColor BLACK = new SimpleColor(0, 0, 0);

    private final double r, g, b;

    public SimpleColor(int full) {
        this.r = (full >> 16) & 0xFF;
        this.g = (full >> 8) & 0xFF;
        this.b = full & 0xFF;
    }

    private SimpleColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    private SimpleColor(double r, double g, double b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public SimpleColor blendWith(SimpleColor other, double ratio) {
        double rNew = blendWith(this.r, other.r, ratio);
        double gNew = blendWith(this.g, other.g, ratio);
        double bNew = blendWith(this.b, other.b, ratio);

        return new SimpleColor(rNew, gNew, bNew);
    }

    private double blendWith(double v1, double v2, double ratio) {
        if (ratio > 1) {
            System.out.println(ratio);
            System.exit(1);
        }
        return v1 * ratio + v2 * (1 - ratio);
    }

    public SimpleColor shaderMultiply(double colorShader) {
        return new SimpleColor(
                this.r * colorShader,
                this.g * colorShader,
                this.b * colorShader
        );
    }

    public Color toJavaFxColor() {
        return Color.color(toDouble(r), toDouble(g), toDouble(b));
    }

    private double toDouble(double v) {
        return Math.max(Math.min(1.0f, v / 255.0f), 0.0);
    }

    @Override
    public String toString() {
        return "SimpleColor{" +
                "r=" + r +
                ", g=" + g +
                ", b=" + b +
                '}';
    }

    public SimpleColor highlight() {
        return new SimpleColor(
                ((int) this.r) ^ 210,
                this.g,
                this.b
        );

    }
}
