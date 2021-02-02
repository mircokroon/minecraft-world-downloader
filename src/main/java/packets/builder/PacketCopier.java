package packets.builder;

import packets.DataTypeProvider;

public class PacketCopier {
    private PacketBuilder builder;
    private DataTypeProvider dataTypeProvider;

    public PacketCopier(int packetId, DataTypeProvider dataTypeProvider) {
        this.dataTypeProvider = dataTypeProvider;
        this.builder = new PacketBuilder(packetId);
    }

    public int copyInt() {
        int val = dataTypeProvider.readInt();
        this.builder.writeInt(val);
        return val;
    }

    public int copyVarInt() {
        int val = dataTypeProvider.readVarInt();
        this.builder.writeVarInt(val);
        return val;
    }

    public boolean copyBool() {
        boolean val = dataTypeProvider.readBoolean();
        this.builder.writeBoolean(val);
        return val;
    }

    public byte copyByte() {
        byte val = dataTypeProvider.readNext();
        this.builder.writeByte(val);
        return val;
    }

}
