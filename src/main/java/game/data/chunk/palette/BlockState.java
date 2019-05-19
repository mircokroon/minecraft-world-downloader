package game.data.chunk.palette;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.StringTag;

import java.util.Map;

public class BlockState {
    String name;
    Map<String, String> properties;

    public BlockState(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = properties;
    }

    public CompoundTag toNbt() {
        CompoundTag rootTag = new CompoundTag();
        rootTag.add("Name", new StringTag(name));

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
