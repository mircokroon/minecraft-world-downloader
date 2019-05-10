package packets;

import game.Coordinates;

public interface DataTypeProvider {
    byte[] readByteArray(int size);
    long readVarLong();
    long readLong();
    int readVarInt();
    String readString();
    void skip(int amount);
    int readShort();
    byte readNext();
    boolean hasNext();
    Coordinates readCoordinates();
}
