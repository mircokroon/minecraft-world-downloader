package packets.handler.plugins;

import java.util.HashMap;
import java.util.Map;
import packets.DataTypeProvider;
import packets.handler.PacketOperator;
import game.data.registries.modded.ForgeRegistryHandler;

public class PluginChannelHandler1_12 extends PluginChannelHandler {
    private final static String FORGE_CHANNEL = "FML|HS";
    private final Map<String, PacketOperator> operators;

    public PluginChannelHandler1_12() {
        this.operators = new HashMap<>();
    }

    @Override
    public void handleCustomPayload(DataTypeProvider provider) {
        String channel = provider.readString();

        if (channel.equals(FORGE_CHANNEL)) {
            this.operators.computeIfAbsent(channel, s -> new ForgeRegistryHandler()).apply(provider);
        }
    }
}
