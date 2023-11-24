package packets.version;

import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

import java.io.DataInputStream;
import java.io.InputStream;

public class DataTypeProvider_1_20_2 extends DataTypeProvider_1_14 {
    public DataTypeProvider_1_20_2(byte[] finalFullPacket) {
        super(finalFullPacket);
    }

    public SpecificTag readNbtTag() {
        try {
            return (SpecificTag) SpecificTag.read(readNext(), new DataInputStream(new InputStream() {
                @Override
                public int read() {
                    return readNext() & 0xFF;
                }
            })).unpack();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public DataTypeProvider ofLength(int length) {
        return new DataTypeProvider_1_20_2(this.readByteArray(length));
    }
}
