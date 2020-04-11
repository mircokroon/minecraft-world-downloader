package packets;

import game.Game;
import game.data.Coordinate3D;
import game.data.container.Slot;
import packets.version.DataTypeProvider_1_14;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to provide an interface between the raw byte data and the various data types. Most methods are
 * self-explanatory.
 */
public class DataTypeProvider {
    private byte[] finalFullPacket;
    private int pos;

    public DataTypeProvider(byte[] finalFullPacket) {
        this.finalFullPacket = finalFullPacket;
        this.pos = 0;
    }

    public static DataTypeProvider ofPacket(byte[] finalFullPacket) {
        if (Game.getProtocolVersion() > 404) {
            return new DataTypeProvider_1_14(finalFullPacket);
        } else {
            return new DataTypeProvider(finalFullPacket);
        }
    }

    public DataTypeProvider ofLength(int length) {
        return new DataTypeProvider(this.readByteArray(length));
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

    public boolean hasNext() {
        return pos < finalFullPacket.length;
    }

    public byte readNext() {
        return finalFullPacket[pos++];
    }

    public int readInt() {
        byte[] bytes = readByteArray(4);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    public byte[] readByteArray(int size) {
        byte[] res = new byte[size];

        System.arraycopy(finalFullPacket, pos, res, 0, size);
        pos += size;

        return res;
    }

    public boolean readBoolean() {
        return readNext() == (byte) 0x01;
    }

    public String readString() {
        int stringSize = readVarInt();

        StringBuilder sb = new StringBuilder();
        while (stringSize-- > 0) {
            sb.appendCodePoint(readNext() & 0xFF);
        }
        return sb.toString();
    }

    public int readVarInt() {
        return DataReader.readVarInt(this::hasNext, this::readNext);
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

    public Coordinate3D readCoordinates() {
        long var = readLong();
        int mask = 0x3FFFFFF;
        int x = (int) (var >> 38) & mask;
        int y = (int) (var >> 26) & 0xFFF;
        int z = (int) var & mask;

        if (x >= 2 << 24) { x -= 2 << 25; }
        if (y >= 2 << 10) { y -= 2 << 11; }
        if (z >= 2 << 24) { z -= 2 << 25; }

        return new Coordinate3D(x, y, z);
    }

    public long readLong() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    public long[] readLongArray(int size) {
        long[] res = new long[size];
        for (int i = 0; i < size; i++) {
            res[i] = readLong();
        }
        return res;
    }

    public int[] readIntArray(int size) {
        int[] res = new int[size];
        for (int i = 0; i < size; i++) {
            res[i] = readInt();
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

    public SpecificTag readNbtTag() {
        try {
            return (SpecificTag) NamedTag.read(new DataInputStream(new InputStream() {
                @Override
                public int read() {
                    return readNext() & 0xFF;
                }
            })).unpack();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public double readFloat() {
        byte[] bytes = readByteArray(4);
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getFloat();
    }

    public double readDouble() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getDouble();
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public String readChat() {
        return readString();
    }
    public String readOptChat() {
        if (readBoolean()) {
            return readChat();
        }
        return null;
    }

    public List<Slot> readSlots(int count) {
        List<Slot> slots = new ArrayList<>(count);

        while (count-- > 0) {
            slots.add(readSlot());
        }

        return slots;
    }

    public Slot readSlot() {
        if (readBoolean()) {
            return new Slot(readVarInt(), readNext(), readNbtTag());
        }
        return null;
    }
}
