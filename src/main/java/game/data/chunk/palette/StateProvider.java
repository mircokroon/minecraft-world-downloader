package game.data.chunk.palette;

/**
 * Interface for data source of palettes.
 */
public interface StateProvider {
    State getState(int i);

    State getDefaultState();
}
