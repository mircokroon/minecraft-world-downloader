package game.data.chunk.palette;

import com.google.gson.JsonPrimitive;
import game.data.WorldManager;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.StringTag;

import java.util.Map;

/**
 * A block state in the global palette (1.13+).
 */
public class BlockState {
    private final String name;
    private final int id;
    private final CompoundTag properties;

    public BlockState(String name, int id, CompoundTag properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;
    }

    public String getProperty(String name) {
        return properties.get(name).stringValue();
    }

    public boolean isChest() {
        return name.equals("minecraft:chest") || name.equals("minecraft:trapped_chest");
    }
    public boolean isDoubleChest() {
        if (!isChest()) {
            return false;
        }

        String type = properties.get("type").stringValue();
        return type != null && (type.equals("left") || type.equals("right"));
    }

    public boolean hasProperty(String property) {
        return !properties.get(property).isError();
    }

    /**
     * Convert this palette state to NBT format.
     */
    public CompoundTag toNbt() {
        CompoundTag rootTag = new CompoundTag();
        rootTag.add("Name", new StringTag(name));

        // only add the properties tag if there are any
        if (properties != null && !properties.isEmpty())  {
            rootTag.add("Properties", properties);
        }


        return rootTag;
    }

    /**
     * Gets the color of this block using the BlockColors object from the world manager.
     * @return the color of the block in integer format, one byte per color.
     */
    public int getColor() {
        return WorldManager.getInstance().getBlockColors().getColor(name);
    }

    public boolean isWater() {
        return name.equals("minecraft:water");
    }

    public boolean isSolid() {
        return WorldManager.getInstance().getBlockColors().isSolid(name);
    }

    public int getNumericId() {
        return id;
    }

    @Override
    public String toString() {
        return "BlockState{" +
            "name='" + name + '\'' +
            ", id=" + id +
            ", properties=" + properties +
            '}';
    }
}
