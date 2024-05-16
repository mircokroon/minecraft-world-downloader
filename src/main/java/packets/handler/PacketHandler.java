package packets.handler;

import game.protocol.HandshakeProtocol;
import game.protocol.Protocol;
import packets.DataProvider;
import packets.DataTypeProvider;
import proxy.ConnectionManager;

import javax.naming.SizeLimitExceededException;
import java.util.Map;

/**
 * Family of classes to handle incoming packets and perform appropriate actions based on the packet type and contents.
 */
public abstract class PacketHandler {
    private final ConnectionManager connectionManager;

    protected static Protocol protocol = new HandshakeProtocol();

    public static void setProtocol(Protocol protocol) {
        PacketHandler.protocol = protocol;
    }

    private DataProvider reader;

    public PacketHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    protected ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Build the given packet, will generate a type provider to parse the contents of the packages to real values. Will
     * determine if the packet is to be forwarded using its return value.
     *
     * @param size the size of the packet to build
     * @return true if the packet should be forwarded, otherwise false.
     */
    public final boolean handle(int size) {
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

    public int indexOf(byte[] outerArray, byte[] smallerArray) {
        for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i+j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    public abstract Map<String, PacketOperator> getOperators();

    public abstract boolean isClientBound();

    public void setReader(DataProvider reader) {
        this.reader = reader;
        this.reader.setCompressionManager(connectionManager.getCompressionManager());
    }
}
