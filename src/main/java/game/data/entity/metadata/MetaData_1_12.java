package game.data.entity.metadata;

import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MetaData_1_12 extends MetaData {
    private static Map<Integer, Consumer<DataTypeProvider>> typeHandlers;
    static {
        typeHandlers = new HashMap<>();
        typeHandlers.put(0, DataTypeProvider::readNext);
        typeHandlers.put(1, DataTypeProvider::readVarInt);
        typeHandlers.put(2, DataTypeProvider::readFloat);
        typeHandlers.put(3, DataTypeProvider::readString);
        typeHandlers.put(4, DataTypeProvider::readChat);
        typeHandlers.put(5, DataTypeProvider::readSlot);
        typeHandlers.put(6, DataTypeProvider::readBoolean);
    }

    private int air;
    private String customName;

    @Override
    public Consumer<DataTypeProvider> getTypeHandler(int i) {
        return typeHandlers.getOrDefault(i, null);
    }

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        switch (i) {
            case 1: return provider -> this.air = provider.readVarInt();
            case 2: return provider -> this.customName = provider.readString();
        }
        return null;
    }

    @Override
    public void addNbtTags(CompoundTag nbt) {
        nbt.add("Air", new IntTag(air));
        if (customName != null && customName.length() > 0) {
            nbt.add("CustomName", new StringTag(customName));
        }
    }
}
