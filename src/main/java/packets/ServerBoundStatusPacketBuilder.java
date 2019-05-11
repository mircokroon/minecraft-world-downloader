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
        }
        return true;
    }
}
