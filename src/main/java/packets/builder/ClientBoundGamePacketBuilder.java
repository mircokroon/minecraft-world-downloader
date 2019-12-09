package packets.builder;

import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.Dimension;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import se.llbit.nbt.SpecificTag;

import java.util.HashMap;
import java.util.Map;

public class ClientBoundGamePacketBuilder extends PacketBuilder {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ClientBoundGamePacketBuilder() {
        operations.put("join_game", provider -> {
            provider.readInt();
            provider.readNext();

            Game.setDimension(Dimension.fromId(provider.readInt()));

            return true;
        });
        operations.put("respawn", provider -> {
            Game.setDimension(Dimension.fromId(provider.readInt()));
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
            WorldManager.unloadChunk(new Coordinate2D(provider.readInt(), provider.readInt()));
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
