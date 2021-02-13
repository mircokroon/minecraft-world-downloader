package game.data.chunk.version;

import javafx.scene.paint.Color;

public class ColorTransformer {

    private static final int HIGHLIGHT_VAL = 210;
    private static final int HIGHLIGHT_MASK = HIGHLIGHT_VAL << 16;

    public int highlight(int rgb) {
        return rgb ^ HIGHLIGHT_MASK;
    }

    public Color highlight(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        return Color.color((red ^ HIGHLIGHT_VAL) / 255.0f, color.getGreen(), color.getBlue());
    }

    public int blendWith(int base, int blender, double ratio) {
        double r = blendWith(getR(base), getR(blender), ratio);
        double g = blendWith(getG(base), getG(blender), ratio);
        double b = blendWith(getB(base), getB(blender), ratio);

        return assemble(r, g, b);
    }

    private double blendWith(double v1, double v2, double ratio) {
        return v1 * ratio + v2 * (1 - ratio);
    }

    public int shaderMultiply(int color, double modifier) {
        if (modifier == 1) { return color; }

        return assemble(getR(color) * modifier, getG(color) * modifier, getB(color) * modifier);
    }

    private int assemble(double r, double g, double b) {
        return clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private int clamp(double v) {
        return Math.max(Math.min(0xFF, (int) Math.sqrt(v)), 0);
    }

    private double getR(int color) {
        return (color >> 16) & 0xFF;
    }

    private double getG(int color) {
        return (color >> 8) & 0xFF;
    }

    private double getB(int color) {
        return (color & 0xFF);
    }

    public Color toColor(int v) {
        return new Color(getR(v) / 255.0, getG(v) / 255.0, getB(v) / 255.0, 1);
    }


}
