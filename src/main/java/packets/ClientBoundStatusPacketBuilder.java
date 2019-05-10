package packets;

public class ClientBoundStatusPacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case 0x00:
                System.out.println("Server response: " + typeProvider.readString());
                return true;
            case 0x01:
                System.out.println("Pong with value: " + typeProvider.readVarLong());
                return true;
        }
        return super.build(size);
    }
}
