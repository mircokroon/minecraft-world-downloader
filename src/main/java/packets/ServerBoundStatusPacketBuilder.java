package packets;

public class ServerBoundStatusPacketBuilder extends PacketBuilder {
    @Override
    public void build(int size) {
        int packetId = getReader().readVarInt();

        switch (packetId) {
            case 0x00:
                System.out.println("Client requested server status");
                break;
            case 0x01:
                System.out.println("Ping with value: " + getReader().readVarLong());
                break;
        }
        super.build(size);
    }
}
