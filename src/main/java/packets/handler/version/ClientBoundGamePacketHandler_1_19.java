package packets.handler.version;

import static packets.builder.NetworkType.BOOL;
import static packets.builder.NetworkType.BYTE;
import static packets.builder.NetworkType.INT;
import static packets.builder.NetworkType.LONG;
import static packets.builder.NetworkType.STRING;
import static packets.builder.NetworkType.VARINT;

import config.Config;
import game.data.WorldManager;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionRegistry;
import game.protocol.Protocol;
import java.util.Map;
import packets.builder.PacketBuilder;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.Tag;

public class ClientBoundGamePacketHandler_1_19 extends ClientBoundGamePacketHandler_1_18 {
    public ClientBoundGamePacketHandler_1_19(ConnectionManager connectionManager) {
        super(connectionManager);

        Protocol protocol = Config.versionReporter().getProtocol();

        Map<String, PacketOperator> operators = getOperators();
        operators.put("Login", provider -> {
            PacketBuilder replacement = new PacketBuilder(protocol.clientBound("Login"));

            replacement.copy(provider, INT, BOOL, BYTE, BYTE);

            // handle dimension codec
            int numDimensions = provider.readVarInt();
            String[] dimensionNames = provider.readStringArray(numDimensions);

            SpecificTag nbt = provider.readNbtTag();
            DimensionRegistry registry = DimensionRegistry.fromNbt(nbt);
            registry.setDimensionNames(dimensionNames);

            WorldManager.getInstance().setDimensionRegistry(registry);

            String dimensionType = provider.readString();
            // current active dimension
            String worldName = provider.readString();
            Dimension dimension = Dimension.fromString(worldName);
            dimension.setType(dimensionType);
            WorldManager.getInstance().setDimension(dimension);

            replacement.writeVarInt(numDimensions);
            replacement.writeStringArray(dimensionNames);
            replacement.writeNbt(nbt);
            replacement.writeString(dimensionType);
            replacement.writeString(worldName);

            replacement.copy(provider, LONG, VARINT);

            // extend view distance communicated to the client to the given value
            int viewDist = provider.readVarInt();
            replacement.writeVarInt(Math.max(viewDist, Config.getExtendedRenderDistance()));

            replacement.copy(provider, VARINT, BOOL, BOOL, BOOL, BOOL);

            boolean hasLastDeath = provider.readBoolean();
            replacement.writeBoolean(hasLastDeath);

            if (hasLastDeath) {
                replacement.copy(provider, STRING, LONG);
            }

            replacement.copyRemainder(provider);

            getConnectionManager().getEncryptionManager().sendImmediately(replacement);
            return false;
        });

        operators.put("Respawn", provider -> {
            String dimensionType = provider.readString();
            Dimension dimension = Dimension.fromString(provider.readString());
            dimension.setType(dimensionType);

            WorldManager.getInstance().setDimension(dimension);
            WorldManager.getInstance().getEntityRegistry().reset();

            return true;
        });
    }
}