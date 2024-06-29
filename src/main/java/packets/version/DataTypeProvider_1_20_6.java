package packets.version;

import game.data.container.Slot;

public class DataTypeProvider_1_20_6 extends DataTypeProvider_1_20_2 {
    public DataTypeProvider_1_20_6(byte[] finalFullPacket) {
        super(finalFullPacket);
    }

    @Override
    public Slot readSlot() {
        int count = readVarInt();

        // TODO: handle 1.20.6+ item components
        if (count > 0) {
            return new Slot(readVarInt(), (byte) count, null);
        }
        return null;
    }


}
