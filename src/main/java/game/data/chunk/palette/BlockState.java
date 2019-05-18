package game.data.chunk.palette;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;

public class BlockState {
    String name;
    Map<String, String> properties;

    public BlockState(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = properties;
    }

    public CompoundTag toNbt() {
        CompoundMap rootMap = new CompoundMap();
        rootMap.put(new StringTag("Name", name));

        CompoundMap propertyMap = new CompoundMap();
        this.properties.forEach((propertyName, propertyValue) -> {
            propertyMap.put(new StringTag(propertyName, propertyValue));
        });

        if (!propertyMap.isEmpty()) {
            rootMap.put(new CompoundTag("Properties", propertyMap));
        }

        return new CompoundTag("", rootMap);
    }
}
