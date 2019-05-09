package packets;

public class ClientBoundHandshakePacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        System.out.println("INVALID PACKET: client-bound handshake packet");
        return super.build(size);
    }
}
