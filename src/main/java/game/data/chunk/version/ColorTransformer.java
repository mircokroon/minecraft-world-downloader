package game.data.chunk.version;

public class ColorTransformer {
    private static final int HIGHLIGHT_MASK = 210 << 16;

    public int highlight(int rgb) {
        return rgb ^ HIGHLIGHT_MASK;
    }

    public int shaderMultiply(int color, double modifier) {
        if (modifier == 1) { return color; }

        short r = (short) ((color >> 16) & 0xFF);
        short g = (short) ((color >> 8) & 0xFF);
        short b = (short) (color & 0xFF);

        r = (short) Math.max(Math.min(0xFF, modifier * r), 0);
        g = (short) Math.max(Math.min(0xFF, modifier * g), 0);
        b = (short) Math.max(Math.min(0xFF, modifier * b), 0);

        return r << 16 | g << 8 | b;
    }
}
