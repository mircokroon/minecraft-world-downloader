package game.data.chunk.palette;

import game.data.WorldManager;
import game.data.chunk.palette.blending.DiscreteBlendEquation;
import game.data.chunk.palette.blending.IBlendEquation;
import game.data.chunk.palette.blending.SquareRootBlendEquation;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;

/**
 * A block state in the block registry (1.13+).
 */
public class BlockState implements State {
    private static final Map<String, IBlendEquation> transparency;
    static {
        IBlendEquation water = new SquareRootBlendEquation(1.1, 0.6);
        transparency = new HashMap<>();
        transparency.put("minecraft:water", water);
        transparency.put("minecraft:ice", new SquareRootBlendEquation(1.4, 0.8));
        transparency.put("minecraft:lava", new DiscreteBlendEquation(.7, .8, .88, .94, .98));

        transparency.put("minecraft:bubble_column", water);
        transparency.put("minecraft:seagrass", water);
        transparency.put("minecraft:tall_seagrass", water);
        transparency.put("minecraft:kelp", water);
    }

    private final String name;
    private final int id;
    private final CompoundTag properties;

    private final boolean isTransparent;
    private final IBlendEquation transparencyEquation;

    public BlockState(String name, int id, CompoundTag properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;

        this.isTransparent = transparency.containsKey(name);
        this.transparencyEquation = this.isTransparent ? transparency.get(name) : null;
    }

    public String getProperty(String name) {
        return properties.get(name).stringValue();
    }

    public CompoundTag getProperties() {
        return properties;
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

    public String getName() {
        return name;
    }

    public boolean hasProperty(String property) {
        return !properties.get(property).isError();
    }

    /**
     * Convert this palette state to NBT format.
     */
    @Override
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
    public SimpleColor getColor() {
        return WorldManager.getInstance().getBlockColors().getColor(name);
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    public IBlendEquation getTransparencyEquation() {
        return transparencyEquation;
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
            ", properties=" + properties.toString().replaceAll("[\\s\\n\\r\\t]", " ") +
            '}';
    }
}
