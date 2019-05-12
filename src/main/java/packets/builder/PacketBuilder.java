package packets.builder;

import packets.DataProvider;

/**
 * Family of classes to handle incoming packets and perform appropriate actions based on the packet type and contents.
 */
public class PacketBuilder {
    private DataProvider reader;

    /**
     * Build the given packet, will generate a type provider to parse the contents of the packages to real values. Will
     * determine if the packet is to be forwarded using its return value.
     * @param size the size of the packet to build
     * @return true if the packet should be forwarded, otherwise false.
     */
    public boolean build(int size) {
        getReader().withSize(size);
        return true;
    }

    public DataProvider getReader() {
        return reader;
    }

    public void setReader(DataProvider reader) {
        this.reader = reader;
    }
}
