package packets.handler;

import game.Config;
import game.data.Coordinate3D;
import game.data.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;
import game.data.chunk.entity.Entity;
import game.data.chunk.entity.MobEntity;
import game.data.chunk.entity.ObjectEntity;
import game.data.container.Slot;
import game.data.dimension.DimensionCodec;
import proxy.ConnectionManager;
import se.llbit.nbt.SpecificTag;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ClientBoundGamePacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
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
            // older versions
            if (Config.getProtocolVersion() < 735) {
                provider.readInt();
                provider.readNext();
                int dimensionEnum = provider.readInt();

                WorldManager.getInstance().setDimension(Dimension.fromId(dimensionEnum));

                return true;
                // > 1.16
            } else {
                if (Config.getExtendedRenderDistance() > 0) {
                    getConnectionManager().getEncryptionManager().handleJoinPacket(provider);
                    return false;
                }
                return true;
            }
        });

        operations.put("respawn", provider -> {
            if (Config.getProtocolVersion() < 735) {
                int dimensionEnum = provider.readInt();
                WorldManager.getInstance().setDimension(Dimension.fromId(dimensionEnum));
            } else {
                SpecificTag dimensionNbt = provider.readNbtTag();
                Dimension dimension = Dimension.fromString(provider.readString());
                dimension.registerType(dimensionNbt);
                WorldManager.getInstance().setDimension(dimension);
            }
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
        operations.put("chunk_update_light", provider -> {
            // TODO: update chunk light for 1.14
            return true;
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

            Coordinate3D playerPos = new Coordinate3D(x, y, z);
            playerPos.offsetGlobal();
            WorldManager.getInstance().setPlayerPosition(playerPos);

            return true;
        };

        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("open_window", provider -> {
            int windowId = provider.readNext();

            if (Config.getProtocolVersion() < 477) {
                String windowType = provider.readString();
                String windowTitle = provider.readChat();

                int numSlots = provider.readNext() & 0xFF;

                WorldManager.getInstance().getContainerManager().openWindow_1_12(windowId, numSlots, windowTitle);
            } else {
                int windowType = provider.readVarInt();
                String windowTitle = provider.readChat();

                WorldManager.getInstance().getContainerManager().openWindow(windowId, windowType, windowTitle);
            }
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

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return true;
    }
}
