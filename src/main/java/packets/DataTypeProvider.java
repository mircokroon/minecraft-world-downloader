package packets;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;

import game.data.Coordinate3D;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    public byte readNextVerbose() {
        byte next = finalFullPacket[pos++];
        System.out.println("Read: " + Integer.toHexString((int) next));
        return next;
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

    public CompoundTag readCompoundTag() {
        return (CompoundTag) readNbtTag();
    }

    public Tag readNbtTag() {
        try {
            NBTInputStream inputStream = new NBTInputStream(new InputStream() {
                @Override
                public int read() {
                    return readNext() & 0xFF;
                }
            }, false);

            return inputStream.readTag();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    public double readDouble() {
        byte[] bytes = readByteArray(8);
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getDouble();
    }
}
