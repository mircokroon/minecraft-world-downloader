package game;

public class BlockInformation {
    BlockState state;
    int lightSky;
    int lightBlock;

    public BlockInformation(BlockState state, int lightSky, int lightBlock) {
        this.state = state;
        this.lightSky = lightSky;
        this.lightBlock = lightBlock;
    }

    @Override
    public String toString() {
        return "BlockInformation{" +
            "state=" + state +
            ", lightSky=" + lightSky +
            ", lightBlock=" + lightBlock +
            '}';
    }
}
