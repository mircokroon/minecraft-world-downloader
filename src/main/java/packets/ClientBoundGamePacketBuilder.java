package packets;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        try {
            DataTypeProvider typeProvider = getReader().withSize(size);
            int packetId = typeProvider.readVarInt();

            System.out.println("Client bound packet 0x" + Integer.toHexString(packetId) + " of size " + size);
            return true;//super.build(size);

        } catch (Exception ex) {
            return true;
        }
    }
}
