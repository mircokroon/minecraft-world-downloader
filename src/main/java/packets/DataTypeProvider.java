package packets;

public interface DataTypeProvider {
    byte[] readByteArray(int size);
    long readVarLong();
    int readVarInt();
    String readString();
    void skip(int amount);
    int readShort();
    byte readNext();
    boolean hasNext();
}
