package packets.builder;

import game.Game;
import game.data.Coordinate3D;
import packets.DataTypeProvider;

public class ServerBoundGamePacketBuilder extends PacketBuilder {
    private final int PLAYER_POSITION = 0x0D;
    private final int PLAYER_POSITION_LOOK = 0x0E;
    private final int VEHICLE_MOVE = 0x10;

    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case PLAYER_POSITION:
            case PLAYER_POSITION_LOOK:
            case VEHICLE_MOVE:
                double x = typeProvider.readDouble();
                double y = typeProvider.readDouble();
                double z = typeProvider.readDouble();
                Game.setPlayerPosition(new Coordinate3D(x, y, z).offset());
                break;
        }
        return true;// super.build(size);
    }
}

