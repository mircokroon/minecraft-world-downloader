package packets.handler;

import game.Game;
import game.NetworkMode;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundHandshakePacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundHandshakePacketHandler() {
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
            Game.getEncryptionManager().sendMaskedHandshake(protocolVersion, nextMode, getHostExtensions(host));
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

    /**
     * Forge appends some data to the end of the host to indicate the client is running forge. We need to copy
     * this over to the masked handshake packet.
     * @param host the original host sent by the client
     * @return the host extension bit (e.g. \0FML\0)
     */
    private String getHostExtensions(String host) {
        String[] parts = host.split("\0", 2);
        if (parts.length <= 1) { return ""; }

        return "\0" + parts[1];
    }
}
