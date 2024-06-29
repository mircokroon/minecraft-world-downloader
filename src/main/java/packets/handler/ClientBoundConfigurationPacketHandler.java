package packets.handler;

import config.Config;
import config.Option;
import config.Version;
import packets.handler.version.ClientBoundConfigurationPacketHandler_1_20_2;
import packets.handler.version.ClientBoundConfigurationPacketHandler_1_20_6;
import proxy.ConnectionManager;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundConfigurationPacketHandler extends PacketHandler {
    private final HashMap<String, PacketOperator> operations = new HashMap<>();

    public ClientBoundConfigurationPacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    public static PacketHandler of(ConnectionManager connectionManager) {
        return Config.versionReporter().select(PacketHandler.class,
                Option.of(Version.V1_20_6, () -> new ClientBoundConfigurationPacketHandler_1_20_6(connectionManager)),
                Option.of(Version.V1_20_2, () -> new ClientBoundConfigurationPacketHandler_1_20_2(connectionManager)),
                Option.of(Version.ANY, () -> new ClientBoundConfigurationPacketHandler(connectionManager))
        );
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
