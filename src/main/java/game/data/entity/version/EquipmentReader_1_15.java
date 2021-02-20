package game.data.entity.version;

import game.data.container.Slot;
import packets.DataTypeProvider;

public class EquipmentReader_1_15 extends EquipmentReader {
    @Override
    public Slot[] readSlots(Slot[] equipment, DataTypeProvider provider) {
        if (equipment == null) {
            equipment = new Slot[6];
        }

        boolean hasNext;
        do {
            byte slotData = provider.readNext();

            hasNext = (slotData & 0x80) > 0;
            int slotId = (slotData & 0x7f);
            equipment[slotId] = provider.readSlot();
        } while (hasNext);

        return equipment;
    }
}
