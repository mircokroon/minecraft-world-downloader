package packets.builder;

import game.Game;
import game.data.Coordinate3D;
import game.data.CoordinateDim2D;
import game.data.Dimension;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkFactory;
import game.data.chunk.entity.Entity;
import game.data.chunk.entity.MobEntity;
import game.data.chunk.entity.ObjectEntity;
import game.data.container.Slot;
import se.llbit.nbt.SpecificTag;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundGamePacketBuilder() {

        operations.put("entity_metadata", provider -> {
            Entity ent = ChunkFactory.getInstance().getEntity(provider.readVarInt());
            if (ent == null) { return true; }
            ent.parseMetadata(provider);

            // mark chunk as unsaved
            Chunk c = WorldManager.getChunk(ent.getPosition().globalToChunk().addDimension(Game.getDimension()));
            if (c == null) { return true; }

            c.setSaved(false);
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

        // only used in 1.16+
        operations.put("join_game", provider -> {
            provider.readInt();
            provider.readNext();
            provider.readNext();
            provider.readNext();

            int numWorlds = provider.readVarInt();
            String[] worldNames = provider.readStringArray(numWorlds);

            SpecificTag dimensionCodec = provider.readNbtTag();
            SpecificTag dimension = provider.readNbtTag();

            Game.setDimension(Dimension.fromString(provider.readString()));

            return true;
        });
        operations.put("respawn", provider -> {
            SpecificTag dimension = provider.readNbtTag();

            Game.setDimension(Dimension.fromString(provider.readString()));
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
            int windowId = provider.readVarInt();
            int windowType = provider.readVarInt();
            String windowTitle = provider.readChat();

            WorldManager.getContainerManager().openWindow(windowId, windowType, windowTitle);
            return true;
        });
        operations.put("close_window", provider -> {
            WorldManager.getContainerManager().closeWindow(provider.readNext());
            return true;
        });

        operations.put("window_items", provider -> {
            int windowId = provider.readVarInt();
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
