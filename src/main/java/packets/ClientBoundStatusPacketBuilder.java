package packets;

public class ClientBoundStatusPacketBuilder extends PacketBuilder {
    @Override
    public void build(int size) {
        int packetId = getReader().readVarInt();

        switch (packetId) {
            case 0x00:
                System.out.println("Server response: " + getReader().readString());
                break;
            case 0x01:
                System.out.println("Pong with value: " + getReader().readVarLong());
                break;
        }
        super.build(size);
    }
}
