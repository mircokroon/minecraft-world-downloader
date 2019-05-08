package packets;

import game.Game;

public class ServerBoundStatusPacketBuilder extends PacketBuilder {

    DataReader reader;

    @Override
    public DataReader getReader() {
        return reader;
    }

    @Override
    public void setReader(DataReader reader) {
        this.reader = reader;
    }

    public void build(int size) {
        int packetId = reader.readVarInt();


        switch (packetId) {
            case 0x00:
                int protocolVersion = reader.readVarInt();
                String host = reader.readString();
                int port = reader.readShort();
                int nextMode = reader.readVarInt();

                System.out.format("HANDSHAKE: v%d (%s:%d) :: new mode = %d\n", protocolVersion, host, port, nextMode);

                Game.setMode(nextMode);

                break;
            default:
                super.build(size);
        }
    }
}
