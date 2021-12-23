package game.data.chunk.palette;

import se.llbit.nbt.SpecificTag;

/**
 * Interface for data type of palettes.
 */
public interface State {
    SpecificTag toNbt();
}
