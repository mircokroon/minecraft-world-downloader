package packets.handler;

import config.Config;
import config.Option;
import config.Version;
import game.data.WorldManager;
import game.data.container.Slot;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import game.data.entity.EntityRegistry;
import game.data.entity.MobEntity;
import game.data.entity.ObjectEntity;
import packets.handler.version.ClientBoundGamePacketHandler_1_14;
import packets.handler.version.ClientBoundGamePacketHandler_1_15;
import packets.handler.version.ClientBoundGamePacketHandler_1_16;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientBoundGamePacketHandler extends PacketHandler {
    private final HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundGamePacketHandler(ConnectionManager connectionManager) {
        super(connectionManager);

        WorldManager worldManager = WorldManager.getInstance();
        EntityRegistry entityRegistry = WorldManager.getInstance().getEntityRegistry();

        operations.put("entity_data", provider -> {
            entityRegistry.addMetadata(provider);
            return true;
        });

        operations.put("entity_equipment", provider -> {
            entityRegistry.addEquipment(provider);
            return true;
        });

        operations.put("spawn_mob", provider -> {
            entityRegistry.addEntity(provider, MobEntity::parse);
            return true;
        });

        operations.put("spawn_object", provider -> {
            entityRegistry.addEntity(provider, ObjectEntity::parse);
            return true;
        });

        operations.put("spawn_player", provider -> {
            entityRegistry.addPlayer(provider);
            return true;
        });

        operations.put("destroy_entities", provider -> {
            entityRegistry.destroyEntities(provider);
            return true;
        });

        operations.put("entity_position", provider -> {
            entityRegistry.updatePositionRelative(provider);
            return true;
        });
        operations.put("entity_position_rotation", provider -> {
            entityRegistry.updatePositionRelative(provider);
            return true;
        });
        operations.put("entity_teleport", provider -> {
            entityRegistry.updatePositionAbsolute(provider);
            return true;
        });

        operations.put("map_data", provider -> {
            worldManager.getMapRegistry().readMap(provider);
            return true;
        });

        operations.put("join_game", provider -> {
            provider.readInt();
            provider.readNext();
            int dimensionEnum = provider.readInt();

            worldManager.setDimension(Dimension.fromId(dimensionEnum));

            return true;
        });

        operations.put("respawn", provider -> {
            int dimensionEnum = provider.readInt();
            worldManager.setDimension(Dimension.fromId(dimensionEnum));

            return true;
        });

        operations.put("chunk_data", provider -> {
            try {
                worldManager.getChunkFactory().addChunk(provider);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        });

        operations.put("chunk_unload", provider -> {
            CoordinateDim2D co = new CoordinateDim2D(provider.readInt(), provider.readInt(), WorldManager.getInstance().getDimension());
            worldManager.unloadChunk(co);
            return Config.getExtendedRenderDistance() == 0;
        });

        operations.put("update_block_entity", provider -> {
            Coordinate3D position = provider.readCoordinates();
            byte action = provider.readNext();
            SpecificTag entityData = provider.readNbtTag();

            worldManager.getChunkFactory().updateTileEntity(position, entityData);
            return true;
        });


        PacketOperator updatePlayerPosition = provider -> {
            double x = provider.readDouble();
            double y = provider.readDouble();
            double z = provider.readDouble();

            CoordinateDouble3D playerPos = new CoordinateDouble3D(x, y, z);
            worldManager.setPlayerPosition(playerPos);

            return true;
        };

        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("open_window", provider -> {
            int windowId = provider.readNext();
            String windowType = provider.readString();
            String windowTitle = provider.readChat();

            int numSlots = provider.readNext() & 0xFF;

            worldManager.getContainerManager().openWindow_1_12(windowId, numSlots, windowTitle);

            return true;
        });
        operations.put("close_window", provider -> {
            worldManager.getContainerManager().closeWindow(provider.readNext());
            return true;
        });

        operations.put("window_items", provider -> {
            int windowId = provider.readNext();
            int count = provider.readShort();
            List<Slot> slots = provider.readSlots(count);

            worldManager.getContainerManager().items(windowId, slots);

            return true;
        });

        operations.put("update_view_distance", provider -> {
            System.out.println("Server tried to change view distance to " + provider.readVarInt());
           return false;
        });
    }

    public static PacketHandler of(ConnectionManager connectionManager) {
        return Config.versionReporter().select(PacketHandler.class,
                Option.of(Version.V1_16, () -> new ClientBoundGamePacketHandler_1_16(connectionManager)),
                Option.of(Version.V1_15, () -> new ClientBoundGamePacketHandler_1_15(connectionManager)),
                Option.of(Version.V1_14, () -> new ClientBoundGamePacketHandler_1_14(connectionManager)),
                Option.of(Version.ANY, () -> new ClientBoundGamePacketHandler(connectionManager))
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
