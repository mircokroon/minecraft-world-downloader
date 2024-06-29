package packets.handler.version;

import static packets.builder.NetworkType.BOOL;
import static packets.builder.NetworkType.INT;
import static packets.builder.NetworkType.VARINT;

import config.Config;
import game.NetworkMode;
import game.data.WorldManager;
import game.protocol.Protocol;
import java.util.Arrays;
import java.util.Map;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;

public class ClientBoundGamePacketHandler_1_20_6 extends ClientBoundGamePacketHandler_1_20_2 {

    private WorldManager world;

    public ClientBoundGamePacketHandler_1_20_6(ConnectionManager connectionManager) {
        super(connectionManager);

        Protocol protocol = Config.versionReporter().getProtocol();

        Map<String, PacketOperator> operators = getOperators();

        operators.put("StartConfiguration", dataTypeProvider -> {
            getConnectionManager().setMode(NetworkMode.CONFIGURATION);
            return true;
        });

        operators.put("ContainerSetContent", provider -> {
            int windowId = provider.readNext();

            int stateId = provider.readVarInt();
            int count = provider.readVarInt();
            WorldManager.getInstance().getContainerManager().items(windowId, count, provider);

            return true;
        });

        operators.put("Login", provider -> {
            PacketBuilder replacement = new PacketBuilder(protocol.clientBound("Login"));

            replacement.copy(provider, INT, BOOL); /* playerId, hardcore */

            int numLevels = provider.readVarInt();
            String[] levels = provider.readStringArray(numLevels);

            replacement.writeVarInt(numLevels);
            replacement.writeStringArray(levels);

            replacement.copy(provider, VARINT); /* maxPlayers */

            // extend view distance communicated to the client to the given value
            int viewDist = provider.readVarInt();
            replacement.writeVarInt(Math.max(viewDist, Config.getExtendedRenderDistance()));

            replacement.copy(provider, VARINT, BOOL, BOOL, BOOL); /* simulationDist, reducedDebug, showDeathScreen, limitedCrafting */

            commonInfo(provider, replacement);

            getConnectionManager().getEncryptionManager().sendImmediately(replacement);
            return false;
        });

        operators.put("Respawn", provider -> {
            commonInfo(provider, null);

            return true;
        });
    }

    private void commonInfo(DataTypeProvider provider, PacketBuilder replacement) {
        // handle dimension codec
        int dimensionType = provider.readVarInt();
        String dimensionName = provider.readString();

        world = WorldManager.getInstance();
        world.setDimension(world.getDimensionRegistry().getDimension(dimensionName));
        world.setDimensionType(world.getDimensionRegistry().getDimensionType(dimensionType));

        if (replacement != null) {
            replacement.writeVarInt(dimensionType);
            replacement.writeString(dimensionName);
            replacement.copyRemainder(provider);
        }
    }
}