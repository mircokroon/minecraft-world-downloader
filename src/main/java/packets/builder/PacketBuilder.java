package packets.builder;

import game.protocol.HandshakeProtocol;
import game.protocol.Protocol;
import packets.DataProvider;
import packets.DataTypeProvider;

import java.util.Map;
import javax.naming.SizeLimitExceededException;

/**
 * Family of classes to handle incoming packets and perform appropriate actions based on the packet type and contents.
 */
public abstract class PacketBuilder {
    protected static Protocol protocol = new HandshakeProtocol();

    public static void setProtocol(Protocol protocol) {
        PacketBuilder.protocol = protocol;
    }

    private DataProvider reader;

    /**
     * Build the given packet, will generate a type provider to parse the contents of the packages to real values. Will
     * determine if the packet is to be forwarded using its return value.
     * @param size the size of the packet to build
     * @return true if the packet should be forwarded, otherwise false.
     */
    public final boolean build(int size) {
        DataTypeProvider typeProvider;
        try {
            typeProvider = reader.withSize(size);
        } catch (SizeLimitExceededException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        int packetID = typeProvider.readVarInt();

        String packetType = protocol.get(packetID, isClientBound());
        PacketOperator operator = getOperators().getOrDefault(packetType, null);
        if (operator == null) {
            return true;
        }

        return operator.apply(typeProvider);
    }

    public abstract Map<String, PacketOperator> getOperators();
    public abstract boolean isClientBound();

    public void setReader(DataProvider reader) {
        this.reader = reader;
    }
}
