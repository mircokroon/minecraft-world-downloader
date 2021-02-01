package packets.handler;

import game.Game;
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
import se.llbit.nbt.SpecificTag;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ClientBoundGamePacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundGamePacketHandler() {

        operations.put("entity_metadata", provider -> {
            Entity ent = ChunkFactory.getInstance().getEntity(provider.readVarInt());
            if (ent == null) { return true; }
            ent.parseMetadata(provider);

            // mark chunk as unsaved
            Chunk c = WorldManager.getChunk(ent.getPosition().globalToChunk().addDimension(Game.getDimension()));
            if (c == null) { return true; }

            WorldManager.touchChunk(c);
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

            // older versions
            if (Game.getProtocolVersion() < 735) {
                int dimensionEnum = provider.readInt();

                Game.setDimension(Dimension.fromId(dimensionEnum));

                // > 1.16
            } else {
                provider.readNext();
                provider.readNext();

                int numDimensions = provider.readVarInt();
                String[] dimensionNames = provider.readStringArray(numDimensions);

                WorldManager.setDimensionCodec(DimensionCodec.fromNbt(dimensionNames, provider.readNbtTag()));

                SpecificTag dimensionNbt = provider.readNbtTag();

                Dimension dimension = Dimension.fromString(provider.readString());
                dimension.registerType(dimensionNbt);
                Game.setDimension(dimension);
            }
            return true;
        });

        operations.put("respawn", provider -> {
            if (Game.getProtocolVersion() < 735) {
                int dimensionEnum = provider.readInt();
                Game.setDimension(Dimension.fromId(dimensionEnum));
            } else {
                SpecificTag dimension = provider.readNbtTag();
                Game.setDimension(Dimension.fromString(provider.readString()));
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
            WorldManager.unloadChunk(new CoordinateDim2D(provider.readInt(), provider.readInt(), Game.getDimension()));
            return true;
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
            Game.setPlayerPosition(playerPos);

            return true;
        };

        operations.put("player_position_look", updatePlayerPosition);
        operations.put("player_vehicle_move", updatePlayerPosition);

        operations.put("open_window", provider -> {
            int windowId = provider.readNext();

            if (Game.getProtocolVersion() < 477) {
                String windowType = provider.readString();
                String windowTitle = provider.readChat();

                int numSlots = provider.readNext() & 0xFF;

                WorldManager.getContainerManager().openWindow_1_12(windowId, numSlots, windowTitle);
            } else {
                int windowType = provider.readVarInt();
                String windowTitle = provider.readChat();

                WorldManager.getContainerManager().openWindow(windowId, windowType, windowTitle);
            }
            return true;
        });
        operations.put("close_window", provider -> {
            WorldManager.getContainerManager().closeWindow(provider.readNext());
            return true;
        });

        operations.put("window_items", provider -> {
            int windowId = provider.readNext();
            int count = provider.readShort();
            List<Slot> slots = provider.readSlots(count);

            WorldManager.getContainerManager().items(windowId, slots);

            return true;
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
