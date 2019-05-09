package packets;

public class PacketBuilder {
    private DataReader reader;

    public boolean build(int size) {
        getReader().skip(size);
        return true;
    }

    public DataReader getReader() {
        return reader;
    }

    public void setReader(DataReader reader) {
        this.reader = reader;
    }
}
