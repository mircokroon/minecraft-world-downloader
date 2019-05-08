package packets;

public class ClientBoundHandshakePacketBuilder extends PacketBuilder {
    @Override
    public void build(int size) {
        System.out.println("INVALID PACKET: client-bound handshake packet");
        super.build(size);
    }
}
