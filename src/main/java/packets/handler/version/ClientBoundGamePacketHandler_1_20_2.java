package packets.handler.version;

import config.Config;
import game.data.WorldManager;
import game.data.dimension.Dimension;
import game.data.entity.EntityRegistry;
import game.protocol.Protocol;
import packets.builder.PacketBuilder;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;

import java.util.Map;

import static packets.builder.NetworkType.*;

public class ClientBoundGamePacketHandler_1_20_2 extends ClientBoundGamePacketHandler_1_19 {
    public ClientBoundGamePacketHandler_1_20_2(ConnectionManager connectionManager) {
        super(connectionManager);

        Protocol protocol = Config.versionReporter().getProtocol();
        EntityRegistry entityRegistry = WorldManager.getInstance().getEntityRegistry();

        Map<String, PacketOperator> operators = getOperators();
        operators.put("Login", provider -> {
            PacketBuilder replacement = new PacketBuilder(protocol.clientBound("Login"));

            replacement.copy(provider, INT, BOOL);

            // handle dimension codec
            int numDimensions = provider.readVarInt();
            String[] dimensionNames = provider.readStringArray(numDimensions);
            WorldManager.getInstance().getDimensionRegistry().setDimensionNames(dimensionNames);

            replacement.writeVarInt(numDimensions);
            replacement.writeStringArray(dimensionNames);

            replacement.copy(provider, VARINT);

            // extend view distance communicated to the client to the given value
            int viewDist = provider.readVarInt();
            replacement.writeVarInt(Math.max(viewDist, Config.getExtendedRenderDistance()));

            replacement.copy(provider, VARINT, BOOL, BOOL, BOOL);

            // current active dimension
            String dimensionType = provider.readString();
            String dimensionName = provider.readString();
            Dimension dimension = Dimension.fromString(dimensionName);
            dimension.setType(dimensionType);
            WorldManager.getInstance().setDimension(dimension);
            WorldManager.getInstance().setDimensionType(WorldManager.getInstance().getDimensionRegistry().getDimensionType(dimensionType));

            replacement.writeString(dimensionType);
            replacement.writeString(dimensionName);

            replacement.copy(provider, LONG, BYTE, BYTE, BOOL, BOOL);

            replacement.copyRemainder(provider);

            getConnectionManager().getEncryptionManager().sendImmediately(replacement);
            return false;
        });

        operators.put("PlayerInfoUpdate", provider -> {
            entityRegistry.updatePlayerAction(provider);
            return true;
        });
    }
}