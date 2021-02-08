package packets.builder;

import org.apache.commons.lang3.NotImplementedException;
import packets.DataTypeProvider;

/**
 * Handles coping of the given network types.
 */
public enum NetworkType {
    INT, VARINT,
    LONG, VARLONG,
    BYTE, STRING,
    NBT, BOOL;

    public void copy(DataTypeProvider from, PacketBuilder to) {
        switch (this) {

            case INT:
                to.writeInt(from.readInt());
                break;
            case VARINT:
                to.writeVarInt(from.readVarInt());
                break;
            case LONG:
                to.writeLong(from.readLong());
                break;
            case VARLONG:
                throw new NotImplementedException("Cannot read/write varlongs");
            case BYTE:
                to.writeByte(from.readNext());
                break;
            case STRING:
                to.writeString(from.readString());
                break;
            case NBT:
                to.writeNbt(from.readNbtTag());
                break;
            case BOOL:
                to.writeBoolean(from.readBoolean());
                break;
        }
    }
}
