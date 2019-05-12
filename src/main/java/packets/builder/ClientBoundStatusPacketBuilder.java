package packets.builder;

import packets.DataTypeProvider;

public class ClientBoundStatusPacketBuilder extends PacketBuilder {
    @Override
    public boolean build(int size) {
        DataTypeProvider typeProvider = getReader().withSize(size);
        int packetId = typeProvider.readVarInt();

        switch (packetId) {
            case 0x00:
                System.out.println("Server status: " + typeProvider.readString());
                return true;
        }
        return true;
    }
}
