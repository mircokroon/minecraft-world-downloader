package packets;

import game.Game;
import game.NetworkMode;

public class ServerBoundHandshakePacketBuilder extends PacketBuilder {
    public void build(int size) {
        int packetId = getReader().readVarInt();

        switch (packetId) {
            case 0x00:
                int protocolVersion = getReader().readVarInt();
                String host = getReader().readString();
                int port = getReader().readShort();
                int nextMode = getReader().readVarInt();

                System.out.format("HANDSHAKE: v%d (%s:%d) :: new mode = %d\n", protocolVersion, host, port, nextMode);

                System.out.println("SETTING NEXT MODE TO: " + nextMode);
                switch (nextMode) {
                    case 1: Game.setMode(NetworkMode.STATUS); break;
                    case 2: Game.setMode(NetworkMode.LOGIN); break;
                }

                break;
            default:
                super.build(size);
        }
    }
}
