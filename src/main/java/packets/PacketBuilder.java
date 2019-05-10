package packets;

public class PacketBuilder {
    private DataProvider reader;

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
