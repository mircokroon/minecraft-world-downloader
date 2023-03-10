package game.data.chunk.palette;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;

/**
 * Interface for data source of palettes.
 */
public interface StateProvider {
    State getState(int i);

    State getState(SpecificTag nbt);

    int getStateId(SpecificTag nbt);

    State getDefaultState();
}
