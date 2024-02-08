package packets.handler.version;

import game.data.WorldManager;
import game.data.dimension.DimensionCodec;
import packets.handler.ClientBoundConfigurationPacketHandler;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;

import java.util.Map;

public class ClientBoundConfigurationPacketHandler_1_20_2 extends ClientBoundConfigurationPacketHandler {
    public ClientBoundConfigurationPacketHandler_1_20_2(ConnectionManager connectionManager) {
        super(connectionManager);

        Map<String, PacketOperator> operators = getOperators();
        WorldManager worldManager = WorldManager.getInstance();

        operators.put("RegistryData", provider -> {
            SpecificTag registryData = provider.readNbtTag();
            worldManager.setDimensionCodec(DimensionCodec.fromNbt(registryData));

            return true;
        });
    }
}
