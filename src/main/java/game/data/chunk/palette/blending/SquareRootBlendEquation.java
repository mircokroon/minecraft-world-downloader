package game.data.chunk.palette.blending;

public class SquareRootBlendEquation implements IBlendEquation {
    double alpha;
    double beta;

    public SquareRootBlendEquation(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public double getRatio(int depth) {
        return alpha - beta / Math.sqrt(depth);
    }
}
