package game.data.entity.metadata;

import config.Config;
import config.Version;
import packets.DataTypeProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MetaData_1_19_3 extends MetaData_1_13 {

    private static Map<Integer, Consumer<DataTypeProvider>> typeHandlers;

    static {
        typeHandlers = new HashMap<>();
        typeHandlers.put(0, DataTypeProvider::readNext);
        typeHandlers.put(1, DataTypeProvider::readVarInt);
        typeHandlers.put(2, DataTypeProvider::readVarLong);
        typeHandlers.put(3, DataTypeProvider::readFloat);
        typeHandlers.put(4, DataTypeProvider::readString);
        typeHandlers.put(5, DataTypeProvider::readChat);
        typeHandlers.put(6, DataTypeProvider::readOptChat);
        typeHandlers.put(7, DataTypeProvider::readSlot);
        typeHandlers.put(8, DataTypeProvider::readBoolean);
        typeHandlers.put(9, DataTypeProvider::readBoolean);
        typeHandlers.put(10, DataTypeProvider::readLong);
        typeHandlers.put(11, provider -> {
            if (provider.readBoolean()) {
                provider.readLong();
            }
        });
        typeHandlers.put(12, DataTypeProvider::readVarInt);
        typeHandlers.put(14, DataTypeProvider::readVarInt);
        typeHandlers.put(15, DataTypeProvider::readNbtTag);
        typeHandlers.put(17, (provider -> {
            provider.readVarInt();
            provider.readVarInt();
            provider.readVarInt();
        }));
        typeHandlers.put(18, DataTypeProvider::readOptVarInt);
        typeHandlers.put(19, DataTypeProvider::readVarInt);
    }

    @Override
    public Consumer<DataTypeProvider> getTypeHandler(int i) {

        if (Config.versionReporter().isAtLeast(Version.V1_19_3)) {
            return typeHandlers.getOrDefault(i, null);
        }

        return super.getTypeHandler(i);
    }
}
