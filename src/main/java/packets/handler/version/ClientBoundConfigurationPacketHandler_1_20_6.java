package packets.handler.version;

import game.data.WorldManager;
import java.util.Map;
import packets.handler.ClientBoundConfigurationPacketHandler;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;

public class ClientBoundConfigurationPacketHandler_1_20_6 extends ClientBoundConfigurationPacketHandler {
    public ClientBoundConfigurationPacketHandler_1_20_6(ConnectionManager connectionManager) {
        super(connectionManager);

        Map<String, PacketOperator> operators = getOperators();
        operators.put("RegistryData", provider -> {
            var registry = provider.readRegistry();
            switch (registry.name()) {
                case "minecraft:worldgen/biome" -> WorldManager.getInstance().getDimensionRegistry().loadBiomes(registry);
                case "minecraft:dimension_type" -> WorldManager.getInstance().getDimensionRegistry().loadDimensions(registry);
            }
            return true;
        });
    }
}
