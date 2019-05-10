package packets;

public class ServerBoundGamePacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        System.out.println("Server bound packet 0x" + Integer.toHexString(packetId) + " of size " + size);
        return true;// super.build(size);
    }
}

