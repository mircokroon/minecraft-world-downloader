package packets.handler;

import config.Config;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;
import game.data.entity.Entity;
import game.data.entity.MobEntity;
import game.data.entity.ObjectEntity;
import game.data.container.Slot;
import packets.handler.version.ClientBoundGamePacketHandler_1_14;
import packets.handler.version.ClientBoundGamePacketHandler_1_15;
import packets.handler.version.ClientBoundGamePacketHandler_1_16;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ClientBoundGamePacketHandler extends PacketHandler {
    private final HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundGamePacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        operations.put("entity_metadata", provider -> {
            Entity ent = ChunkFactory.getInstance().getEntity(provider.readVarInt());
            if (ent == null) { return true; }
            ent.parseMetadata(provider);

            // mark chunk as unsaved
            Chunk c = WorldManager.getInstance().getChunk(ent.getPosition().globalToChunk().addDimension(WorldManager.getInstance().getDimension()));
            if (c == null) { return true; }

            WorldManager.getInstance().touchChunk(c);
            return true;
        });

        operations.put("spawn_mob", provider -> {
            ChunkFactory.getInstance().addEntity(provider, MobEntity::parse);

            return true;
        });

        operations.put("spawn_object", provider -> {
            ChunkFactory.getInstance().addEntity(provider, ObjectEntity::parse);

            return true;
        });

        operations.put("join_game", provider -> {
            provider.readInt();
            provider.readNext();
            int dimensionEnum = provider.readInt();

            WorldManager.getInstance().setDimension(Dimension.fromId(dimensionEnum));

            return true;
        });

        operations.put("respawn", provider -> {
            int dimensionEnum = provider.readInt();
            WorldManager.getInstance().setDimension(Dimension.fromId(dimensionEnum));

            return true;
        });

        operations.put("chunk_data", provider -> {
            try {
                ChunkFactory.getInstance().addChunk(provider);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        });

        operations.put("chunk_unload", provider -> {
            CoordinateDim2D co = new CoordinateDim2D(provider.readInt(), provider.readInt(), WorldManager.getInstance().getDimension());
            WorldManager.getInstance().unloadChunk(co);
            return Config.getExtendedRenderDistance() == 0;
        });

        operations.put("update_block_entity", provider -> {
            Coordinate3D position = provider.readCoordinates();
            byte action = provider.readNext();
            SpecificTag entityData = provider.readNbtTag();

            ChunkFactory.getInstance().updateTileEntity(position, entityData);
            return true;
        });


        PacketOperator updatePlayerPosition = provider -> {
            double x = provider.readDouble();
            double y = provider.readDouble();
            double z = provider.readDouble();

            CoordinateDouble3D playerPos = new CoordinateDouble3D(x, y, z);
            WorldManager.getInstance().setPlayerPosition(playerPos);

            return true;
        };

        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("open_window", provider -> {
            int windowId = provider.readNext();
            String windowType = provider.readString();
            String windowTitle = provider.readChat();

            int numSlots = provider.readNext() & 0xFF;

            WorldManager.getInstance().getContainerManager().openWindow_1_12(windowId, numSlots, windowTitle);

            return true;
        });
        operations.put("close_window", provider -> {
            WorldManager.getInstance().getContainerManager().closeWindow(provider.readNext());
            return true;
        });

        operations.put("window_items", provider -> {
            int windowId = provider.readNext();
            int count = provider.readShort();
            List<Slot> slots = provider.readSlots(count);

            WorldManager.getInstance().getContainerManager().items(windowId, slots);

            return true;
        });

        operations.put("update_view_distance", provider -> {
            System.out.println("Server tried to change view distance to " + provider.readVarInt());
           return false;
        });
    }

    public static PacketHandler of(ConnectionManager connectionManager) {
        final int VERSION_1_14 = 441;
        final int VERSION_1_15 = 573;
        final int VERSION_1_16 = 701;

        int protocolVersion = Config.getProtocolVersion();
        if (protocolVersion >= VERSION_1_16) {
            return new ClientBoundGamePacketHandler_1_16(connectionManager);
        } else if (protocolVersion >= VERSION_1_15) {
            return new ClientBoundGamePacketHandler_1_15(connectionManager);
        } else if (protocolVersion >= VERSION_1_14) {
            return new ClientBoundGamePacketHandler_1_14(connectionManager);
        } else {
            return new ClientBoundGamePacketHandler(connectionManager);
        }
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
