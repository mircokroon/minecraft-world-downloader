package packets.handler;

import game.NetworkMode;
import java.util.HashMap;
import java.util.Map;
import proxy.ConnectionManager;

public class ServerBoundConfigurationPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();

    public ServerBoundConfigurationPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("FinishConfiguration", provider -> {
            getConnectionManager().setMode(NetworkMode.GAME);
            return true;
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
