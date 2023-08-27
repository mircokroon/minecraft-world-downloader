package game.data.chunk;

import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.SimpleColor;
import java.util.Objects;

/**
 * Cave class used in image rendering
 */
public class Cave {
    int y;
    int depth;
    BlockState block;

    Cave(int y, BlockState block) {
        this.y = y;
        this.block = block;
        this.depth = 1;
    }

    public void addDepth() {
        depth += 1;
    }

    public int y() { return y; }

    public int depth() { return depth; }

    public BlockState block() { return block; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        var that = (Cave) obj;
        return this.y == that.y &&
            this.depth == that.depth &&
            Objects.equals(this.block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(y, depth, block);
    }

    @Override
    public String toString() {
        return "Cave[" +
            "y=" + y + ", " +
            "depth=" + depth + ", " +
            "block=" + block + ']';
    }

    /**
     * Adjust colour based on height and depth of cave
     */
    public SimpleColor getColor() {
        double brightness = 230 * (0.05 + (Math.log(depth) / Math.log(80)) * 0.9);
        SimpleColor caveDepth = new SimpleColor(10, brightness / 2, brightness)
            .blendWith(new SimpleColor(brightness, 10, 10), map(-80, 100, y));

        SimpleColor blockCol = block.getColor();
        return caveDepth.blendWith(blockCol, .85);
    }

    private double map(double min, double max, double val) {
        if (val < min) { return 0; }
        if (val > max) { return 1; }
        return (val - min) / (max - min);
    }
}
