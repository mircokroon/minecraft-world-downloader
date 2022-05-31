package packets.handler.version;

import java.util.Map;

import game.data.WorldManager;
import game.data.coordinates.Coordinate3D;
import packets.handler.PacketOperator;
import proxy.ConnectionManager;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

public class ClientBoundGamePacketHandler_1_18 extends ClientBoundGamePacketHandler_1_17 {
    public ClientBoundGamePacketHandler_1_18(ConnectionManager connectionManager) {
        super(connectionManager);

        WorldManager worldManager = WorldManager.getInstance();
        Map<String, PacketOperator> operators = getOperators();
        operators.put("LevelChunkWithLight", provider -> {
            try {
                worldManager.getChunkFactory().addChunk(provider);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        });

        operators.put("BlockEntityData", provider -> {
            Coordinate3D position = provider.readCoordinates();
            byte action = provider.readNext();
            SpecificTag entityData = provider.readNbtTag();

            if(entityData instanceof CompoundTag entity) {
                entity.add("id", new StringTag(worldManager.getBlockEntityMap().getBlockEntityName(action)));
            }
            worldManager.getChunkFactory().updateTileEntity(position, entityData);
            return true;
        });
    }

}
