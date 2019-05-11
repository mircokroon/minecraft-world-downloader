package packets;

import game.Game;
import game.NetworkMode;

public class ServerBoundHandshakePacketBuilder extends PacketBuilder {
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case 0x00:
                int protocolVersion = typeProvider.readVarInt();
                String host = typeProvider.readString();
                int port = typeProvider.readShort();
                int nextMode = typeProvider.readVarInt();

                System.out.format("Attempted handshake with %s:%d, protocol version %d :: next state: %d\n", host, port, protocolVersion, nextMode);

                switch (nextMode) {
                    case 1: Game.setMode(NetworkMode.STATUS); break;
                    case 2: Game.setMode(NetworkMode.LOGIN); break;
                }

                Game.getEncryptionManager().sendMaskedHandshake(protocolVersion, nextMode);

                return false;
            default:
                return true;
        }
    }
}
