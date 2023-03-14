package game.data.commandblock;

import java.util.HashMap;
import java.util.Map;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkEntities;
import game.data.chunk.palette.BlockState;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim3D;
import packets.DataTypeProvider;
import packets.builder.Chat;
import packets.builder.MessageTarget;
import packets.builder.PacketBuilder;

public class CommandBlockManager {

    private final Map<CoordinateDim3D, CommandBlock> storedCommandBlocks;

    public CommandBlockManager() {
        storedCommandBlocks = new HashMap<>();
    }

    public void readAndStoreCommandBlock(DataTypeProvider provider) {
        final Coordinate3D coords = provider.readCoordinates();
        final String command = provider.readString();

        // Mode (0 = chain, 1 = repeating, 2 = impulse)
        final int mode = provider.readVarInt();
        final byte flags = provider.readNext();
        
        CommandBlock commandblock = new CommandBlock(coords, command, mode, flags);
        storedCommandBlocks.put(coords.addDimension3D(WorldManager.getInstance().getDimension()), commandblock);

        Chunk c = WorldManager.getInstance().getChunk(coords.globalToChunk().addDimension(WorldManager.getInstance().getDimension()));
        if (c != null) {
            BlockState block = c.getBlockStateAt(coords.withinChunk());
            if (block != null) {
                c.addCommandBlock(commandblock);
                sendCommandBlockMessage(commandblock);
            } else {
                sendCommandBlockFailureMessage(commandblock, "Block not found.");
            }
        } else {
            sendCommandBlockFailureMessage(commandblock, "Chunk not loaded.");
        }
    }
    
    public void loadPreviousCommandBlockAt(ChunkEntities chunk, CoordinateDim3D location) {
        if (storedCommandBlocks.containsKey(location)) {
            chunk.addCommandBlock(storedCommandBlocks.get(location));
        }
    }

    private void sendCommandBlockFailureMessage(CommandBlock commandblock, String cause) {
        if (Config.sendInfoMessages()) {
            Chat message = new Chat("Unable to save command block at " + commandblock.getLocation() + ". " + cause);
            message.setColor("red");

            Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }
    
    private void sendCommandBlockMessage(CommandBlock commandblock) {
        if (Config.sendInfoMessages()) {
            String message = "Recorded command block at " + commandblock.getLocation();
            Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }
}
