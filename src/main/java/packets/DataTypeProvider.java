package packets;

import game.Coordinate3D;

import java.nio.ByteBuffer;

public class DataTypeProvider {
    byte[] finalFullPacket;
    int pos;

    public DataTypeProvider(byte[] finalFullPacket) {
        this.finalFullPacket = finalFullPacket;
        this.pos = 0;
    }

    public byte[] readByteArray(int size) {
        byte[] res = new byte[size];

        System.arraycopy(finalFullPacket, pos, res, 0, size);
        pos += size;

        return res;
    }

    public long readVarLong() {
        int numRead = 0;
        long result = 0;
        byte read;
        do {
            if (!hasNext()) {
                throw new RuntimeException("VarLong lacks bytes! We may be out of sync now.");
            }
            read = readNext();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 10) {
                throw new RuntimeException("VarLong is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public long readLong() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    public int readInt() {
        byte[] bytes = readByteArray(4);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    public boolean readBoolean() {
        return readNext() == (byte) 0x01;
    }

    public int readVarInt() {
        return DataReader.readVarInt(this::hasNext, this::readNext);
    }

    public String readString() {
        int stringSize = readVarInt();

        StringBuilder sb = new StringBuilder();
        while (stringSize-- > 0) {
            sb.appendCodePoint(readNext());
        }
        return sb.toString();
    }

    public void skip(int amount) {
        while (amount-- > 0) {
            readNext();
        }
    }

    public int readShort() {
        byte low = readNext();
        byte high = readNext();
        return (((low & 0xFF) << 8) | (high & 0xFF));
    }

    public byte readNext() {
        return finalFullPacket[pos++];
    }

    public boolean hasNext() {
        return pos < finalFullPacket.length;
    }

    public Coordinate3D readCoordinates() {
        long val = readLong();
        int x = (int) (val >> 38);
        int y = (int) (val >> 26) & 0xFFF;
        int z = (int) (val << 38 >> 38);
        return new Coordinate3D(x, y, z);
    }

    public long[] readLongArray(int size) {
        long[] res = new long[size];
        for (int i = 0; i < size; i++) {
            res[i] = readLong();
        }
        return res;
    }

    public int[] readVarIntArray(int size) {
        int[] res = new int[size];
        for (int i = 0; i < size; i++) {
            res[i] = readVarInt();
        }
        return res;
    }
    /*
    byte[] readByteArray(int size);
    long readVarLong();
    long readLong();
    int readVarInt();
    String readString();
    void skip(int amount);
    int readShort();
    int readInt();
    boolean readBoolean();
    byte readNext();
    boolean hasNext();
    Coordinate3D readCoordinates();
    long[] readLongArray(int size);

     */
}
