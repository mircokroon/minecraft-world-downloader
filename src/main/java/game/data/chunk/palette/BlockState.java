package game.data.chunk.palette;

import game.data.WorldManager;
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

    /**
     * Gets the color of this block using the BlockColors object from the world manager.
     * @return the color of the block in integer format, one byte per color.
     */
    public int getColor() {
        return WorldManager.getBlockColors().getColor(name);
    }

    public boolean isWater() {
        return name.equals("minecraft:water");
    }

    public boolean isSolid() {
        return WorldManager.getBlockColors().isSolid(name);
    }
}
