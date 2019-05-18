package packets.builder;

import game.Game;
import game.NetworkMode;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundHandshakePacketBuilder extends PacketBuilder {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundHandshakePacketBuilder() {
        operations.put("handshake", provider -> {
            int protocolVersion = provider.readVarInt();
            String host = provider.readString();
            int port = provider.readShort();
            int nextMode = provider.readVarInt();

            switch (nextMode) {
                case 1:
                    Game.setMode(NetworkMode.STATUS);
                    break;
                case 2:
                    Game.setMode(NetworkMode.LOGIN);
                    break;
            }

            Game.setProtocolVersion(protocolVersion);
            Game.getEncryptionManager().sendMaskedHandshake(protocolVersion, nextMode);
            return false;
        });
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return false;
    }
}
