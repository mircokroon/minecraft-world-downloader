package game.data.chunk.palette;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.StringTag;

import java.util.Map;

/**
 * A block state in the global palette (1.13+).
 */
public class BlockState {
    private String name;
    private Map<String, String> properties;

    public BlockState(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * Convert this palette state to NBT format.
     */
    public CompoundTag toNbt() {
        CompoundTag rootTag = new CompoundTag();
        rootTag.add("Name", new StringTag(name));

        // only add the properties tag if there are any
        if (properties != null && !properties.isEmpty())  {
            CompoundTag propertyTag = new CompoundTag();
            this.properties.forEach((propertyName, propertyValue) -> {
                propertyTag.add(propertyName, new StringTag(propertyValue));
            });

            rootTag.add("Properties", propertyTag);
        }


        return rootTag;
    }
}
