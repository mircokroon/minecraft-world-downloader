package packets;

public class ServerBoundStatusPacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);

        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case 0x00:
                System.out.println("Client requested server status");
                return true;
            case 0x01:
                System.out.println("Ping with value: " + typeProvider.readVarLong());
                return true;
        }
        return super.build(size);
    }
}
