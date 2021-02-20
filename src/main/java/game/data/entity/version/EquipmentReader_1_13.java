package game.data.entity.version;

import game.data.container.Slot;
import packets.DataTypeProvider;

public class EquipmentReader_1_13 extends EquipmentReader {

    @Override
    public Slot[] readSlots(Slot[] equipment, DataTypeProvider provider) {
        if (equipment == null) {
            equipment = new Slot[6];
        }

        int slotId = provider.readNext() & 0xff;
        equipment[slotId] = provider.readSlot();

        return equipment;
    }
}
