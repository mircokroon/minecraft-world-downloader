package game.data.chunk.version;

public class ColorTransformer {

    private static final int HIGHLIGHT_MASK = 210 << 16;

    public int highlight(int rgb) {
        return rgb ^ HIGHLIGHT_MASK;
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
        return Math.pow((color >> 16) & 0xFF, 2);
    }

    private double getG(int color) {
        return Math.pow((color >> 8) & 0xFF, 2);
    }

    private double getB(int color) {
        return Math.pow((color & 0xFF), 2);
    }


}
