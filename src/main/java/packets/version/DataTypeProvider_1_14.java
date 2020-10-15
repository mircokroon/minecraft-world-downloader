package packets.version;

import game.data.Coordinate3D;
import packets.DataTypeProvider;

public class DataTypeProvider_1_14 extends DataTypeProvider_1_13 {
    public DataTypeProvider_1_14(byte[] finalFullPacket) {
        super(finalFullPacket);
    }

    @Override
    public Coordinate3D readCoordinates() {
        long val = readLong();
        int x = (int) (val >> 38);
        int y = (int) (val & 0xFFF);
        int z = (int) ((val << 38 >> 38) >> 12);
        return new Coordinate3D(x, y, z);
    }

    @Override
    public DataTypeProvider ofLength(int length) {
        return new DataTypeProvider_1_14(this.readByteArray(length));
    }
}
