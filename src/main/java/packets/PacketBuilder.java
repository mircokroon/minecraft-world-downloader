package packets;

public class PacketBuilder {
    private DataReader reader;

    public void build(int size) {
        getReader().skip(size);
    }

    public DataReader getReader() {
        return reader;
    }

    public void setReader(DataReader reader) {
        this.reader = reader;
    }
}