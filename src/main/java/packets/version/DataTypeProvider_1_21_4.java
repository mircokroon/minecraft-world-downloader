package packets.version;

import game.data.container.Slot;
import packets.DataTypeProvider;

public class DataTypeProvider_1_21_4 extends DataTypeProvider_1_20_6 {
    public DataTypeProvider_1_21_4(byte[] finalFullPacket) {
        super(finalFullPacket);
    }

    @Override
    public DataTypeProvider ofLength(int length) {
        return new DataTypeProvider_1_21_4(this.readByteArray(length));
    }

    @Override
    public Slot readSlot() {
        if (!readBoolean()) {
            return null; // no item
        }

        int itemId = readVarInt();
        byte count = readNext();

        // Item components (new in 1.21+)
        int numComponents = readVarInt();
        for (int i = 0; i < numComponents; i++) {
            // TODO: actually parse item components
            readNbtTag(); // placeholder
        }

        return new Slot(itemId, count, null);
    }
}