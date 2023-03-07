package game.data.entity.metadata;

import config.Config;
import config.Option;
import config.Version;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;

import java.util.function.Consumer;

public abstract class MetaData {
    private final static int TERMINATOR = 0xFF;
    public abstract void addNbtTags(CompoundTag nbt);

    MetaData() { }

    public void parse(DataTypeProvider provider) {
        while (true) {
            int index = provider.readNext() & 0xFF;
            if (index == TERMINATOR) { break; }

            int type = provider.readVarInt();

            Consumer<DataTypeProvider> indexHandler = getIndexHandler(index);
            Consumer<DataTypeProvider> typeHandler = getTypeHandler(type);

            if (indexHandler == null && typeHandler == null) { break; }

            if (indexHandler != null) {
                indexHandler.accept(provider);
            } else {
                typeHandler.accept(provider);
            }
        }
    }

    /**
     * Returns a MetaData object of the correct version.
     * @return the metadata matching the given version
     */
    public static MetaData getVersionedMetaData() {
        return Config.versionReporter().select(MetaData.class,
                Option.of(Version.V1_19_3, MetaData_1_19_3::new),
                Option.of(Version.V1_13, MetaData_1_13::new),
                Option.of(Version.ANY, MetaData_1_12::new)
        );

//        if (Config.getProtocolVersion() >= 341) {
//            return new MetaData_1_13();
//        } else {
//            return new MetaData_1_12();
//        }
    }

    public abstract Consumer<DataTypeProvider> getTypeHandler(int i);
    public abstract Consumer<DataTypeProvider> getIndexHandler(int i);
}
