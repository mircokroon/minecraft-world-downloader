package packets.handler.version;

import config.Config;
import game.data.WorldManager;
import game.data.coordinates.Coordinate3D;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionCodec;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import packets.builder.PacketBuilder;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;

import java.util.Map;

import static packets.builder.NetworkType.*;

public class ClientBoundGamePacketHandler_1_16 extends ClientBoundGamePacketHandler_1_14 {
    public ClientBoundGamePacketHandler_1_16(ConnectionManager connectionManager) {
        super(connectionManager);

        Protocol protocol = Config.versionReporter().getProtocol();

        Map<String, PacketOperator> operators = getOperators();
        operators.put("join_game", provider -> {
            PacketBuilder replacement = new PacketBuilder(protocol.clientBound("join_game"));

            replacement.copy(provider, INT, BOOL, BYTE, BYTE);

            // handle dimension codec
            int numDimensions = provider.readVarInt();
            String[] dimensionNames = provider.readStringArray(numDimensions);

            SpecificTag dimensionCodec = provider.readNbtTag();
            WorldManager.getInstance().setDimensionCodec(DimensionCodec.fromNbt(dimensionNames, dimensionCodec));

            SpecificTag dimensionNbt = provider.readNbtTag();

            // current active dimension
            String worldName = provider.readString();
            Dimension dimension = Dimension.fromString(worldName);
            dimension.registerType(dimensionNbt);
            WorldManager.getInstance().setDimension(dimension);

            replacement.writeVarInt(numDimensions);
            replacement.writeStringArray(dimensionNames);
            replacement.writeNbt(dimensionCodec);
            replacement.writeNbt(dimensionNbt);
            replacement.writeString(worldName);

            replacement.copy(provider, LONG, VARINT);

            // extend view distance communicated to the client to the given value
            int viewDist = provider.readVarInt();
            WorldManager.getInstance().setServerRenderDistance(viewDist);
            replacement.writeVarInt(Math.max(viewDist, Config.getExtendedRenderDistance()));

            replacement.copy(provider, BOOL, BOOL, BOOL, BOOL);

            getConnectionManager().getEncryptionManager().sendImmediately(replacement);
            return false;
        });

        operators.put("respawn", provider -> {
            SpecificTag dimensionNbt = provider.readNbtTag();
            Dimension dimension = Dimension.fromString(provider.readString());
            dimension.registerType(dimensionNbt);
            WorldManager.getInstance().setDimension(dimension);
            WorldManager.getInstance().getEntityRegistry().reset();

            return true;
        });

        operators.put("chunk_multi_block_change", provider -> {
            Coordinate3D pos = provider.readSectionCoordinates();
            WorldManager.getInstance().multiBlockChange(pos, provider);

            return true;
        });
    }
}
