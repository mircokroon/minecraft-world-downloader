package packets.builder;

import game.Game;
import game.NetworkMode;
import packets.DataTypeProvider;

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

                switch (nextMode) {
                    case 1:
                        Game.setMode(NetworkMode.STATUS);
                        break;
                    case 2:
                        Game.setMode(NetworkMode.LOGIN);
                        break;
                }

                Game.getEncryptionManager().sendMaskedHandshake(protocolVersion, nextMode);
                return false;
            default:
                return true;
        }
    }
}
