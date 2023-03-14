package packets;

import config.Config;
import config.Option;
import config.Version;
import game.data.coordinates.Coordinate3D;
import game.data.container.Slot;
import game.data.container.Slot_1_12;
import game.data.coordinates.CoordinateDouble3D;
import packets.version.DataTypeProvider_1_13;
import packets.version.DataTypeProvider_1_14;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Class to provide an interface between the raw byte data and the various data types. Most methods are
 * self-explanatory.
 */
public class DataTypeProvider {
    private static final int MAX_SHORT_VAL = 1 << 15;
    private byte[] finalFullPacket;
    private int pos;

    public DataTypeProvider(byte[] finalFullPacket) {
        this.finalFullPacket = finalFullPacket;
        this.pos = 0;
    }

    public static DataTypeProvider ofPacket(byte[] finalFullPacket) {
        return Config.versionReporter().select(DataTypeProvider.class,
                Option.of(Version.V1_14, () -> new DataTypeProvider_1_14(finalFullPacket)),
                Option.of(Version.V1_13, () -> new DataTypeProvider_1_13(finalFullPacket)),
                Option.of(Version.ANY, () -> new DataTypeProvider(finalFullPacket))
        );
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
                throw new RuntimeException("Invalid VarLong found! Packet structure may have changed.");
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
        ((Buffer) buffer).flip();
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
        int val = (((low & 0xFF) << 8) | (high & 0xFF));
        return val > MAX_SHORT_VAL ? -(MAX_SHORT_VAL * 2 - val) : val ;
    }

    public Coordinate3D readCoordinates() {
        long var = readLong();
        int mask = 0x3FFFFFF;
        int x = (int) (var >> 38) & mask;
        int y = (int) (var >> 26) & 0xFFF;
        int z = (int) var & mask;

        if (x >= 1 << 25) { x -= 1 << 26; }
        if (y >= 1 << 11) { y -= 1 << 12; }
        if (z >= 1 << 25) { z -= 1 << 26; }

        return new Coordinate3D(x, y, z);
    }

    public Coordinate3D readSectionCoordinates() {
        long val = readLong();
        int x = (int) (val >>> 42);
        int y = (int) (val << 44 >>> 44);
        int z = (int) (val << 22 >>> 42);

        if (x >= 1 << 21) { x -= 1 << 22; }
        if (y >= 1 << 19) { y -= 1 << 20; }
        if (z >= 1 << 21) { z -= 1 << 22; }

        return new Coordinate3D(x, y, z);
    }

    public long readLong() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        ((Buffer) buffer).flip();
        return buffer.getLong();
    }

    public long[] readLongArray(int size, int expected) {
        long[] res = new long[expected];
        for (int i = 0; i < expected; i++) {
            res[i] = readLong();
        }
        for (int i = 0; i < size - expected; i++) {
            readLong();
        }
        return res;
    }

    public long[] readLongArray(int size) {
        return readLongArray(size, size);
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

    public float readFloat() {
        byte[] bytes = readByteArray(4);
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.put(bytes);
        ((Buffer) buffer).flip();
        return buffer.getFloat();
    }

    public double readDouble() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.put(bytes);
        ((Buffer) buffer).flip();
        return buffer.getDouble();
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }
    
    public UUID readOptUUID() {
        if (readBoolean()) {
            return readUUID();
        }
        return null;
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

    /**
     * 1.12 version for slot reading, slightly different from 1.13+
     */
    public Slot readSlot() {
        int itemId = readShort();

        if (itemId == -1) {
            return null;
        }

        return new Slot_1_12(itemId, readNext(), readShort(), readNbtTag());
    }

    public static int readOptVarInt(DataTypeProvider provider) {
        if (provider.readBoolean()) {
            return provider.readVarInt();
        }
        return 0;
    }

    public String[] readStringArray(int size) {
        String[] res = new String[size];
        for (int i = 0; i < size; i++) {
            res[i] = readString();
        }
        return res;
    }

    public BitSet readBitSet() {
        int numLongs = readVarInt();
        long[] longs = readLongArray(numLongs);
        return BitSet.valueOf(longs);
    }

    public CoordinateDouble3D readDoubleCoordinates() {
        return new CoordinateDouble3D(readDouble(), readDouble(), readDouble());
    }

    public DataTypeProvider copy() {
        return new DataTypeProvider(Arrays.copyOf(this.finalFullPacket, this.finalFullPacket.length));
    }

    public int remaining() {
        return this.finalFullPacket.length - pos;
    }

    @Override
    public String toString() {
        return "DataTypeProvider{" +
                "finalFullPacket[" + finalFullPacket.length + "]=" + Arrays.toString(finalFullPacket) +
                ", pos=" + pos +
                '}';
    }
}
