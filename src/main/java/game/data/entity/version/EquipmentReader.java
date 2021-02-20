package game.data.entity.version;

import config.Config;
import config.Option;
import config.Version;
import game.data.container.Slot;
import packets.DataTypeProvider;

public abstract class EquipmentReader {
    public abstract Slot[] readSlots(Slot[] equipment, DataTypeProvider provider);

    public static EquipmentReader getVersioned() {
        return Config.versionReporter().select(EquipmentReader.class,
                Option.of(Version.V1_15, EquipmentReader_1_15::new),
                Option.of(Version.ANY, EquipmentReader_1_13::new)
        );
    }

}
