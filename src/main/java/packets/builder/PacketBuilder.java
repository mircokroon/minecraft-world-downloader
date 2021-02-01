package packets.builder;

import packets.lib.ByteQueue;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PacketBuilder {
    ByteQueue bytes;

    public PacketBuilder(int packetId) {
        this.bytes = new ByteQueue();
        writeVarInt(packetId);
    }

    /**
     * Method to write a varInt to the given list. Based on: https://wiki.vg/Protocol
     * @param value the value to write
     */
    public void writeVarInt(int value) {
        writeVarInt(this.bytes, value);
    }

    /**
     * Method to write a varInt to the given list. Based on: https://wiki.vg/Protocol
     * @param value the value to write
     */
    private static void writeVarInt(ByteQueue destination, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            destination.insert(temp);
        } while (value != 0);
    }

    /**
     * Method to write a string to the given list.
     * @param str   the string to write
     */
    public void writeString(String str) {
        final byte[][] stringBytes = {null};
        stringBytes[0] = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(stringBytes[0].length);
        writeByteArray(stringBytes[0]);
    }

    /**
     * Method to write a byte array to the given list.
     * @param arr the bytes to write
     */
    public void writeByteArray(byte[] arr) {
        for (byte b : arr) {
            bytes.insert(b);
        }
    }

    /**
     * Method to write a the packet length to the start of the given list. This is done by writing the varint to a
     * different ByteQueue first, as we need to know the size before we can prepend it.
     */
    private void prependPacketLength() {
        int len = bytes.size();

        ByteQueue r = new ByteQueue(5);
        writeVarInt(r, len);
        this.bytes.prepend(r);
    }

    public ByteQueue build() {
        prependPacketLength();
        return bytes;
    }

    /**
     * Write a short to the given byte list.
     * @param shortVal the value of the short
     */
    public void writeShort(int shortVal) {
        bytes.insert((byte) ((shortVal >>> 8) & 0xFF));
        bytes.insert((byte) ((shortVal) & 0xFF));
    }

    /**
     * Write an int, needs to split into 4 bytes in the correct order.
     */
    public void writeInt(int val) {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) (val & 0xFF);
        bytes[2] = (byte) (val >> 8 & 0xFF);
        bytes[1] = (byte) (val >> 16 & 0xFF);
        bytes[0] = (byte) (val >> 24 & 0xFF);

        writeByteArray(bytes);
    }

    public void writeBoolean(boolean val) {
        bytes.insert((byte) (val ? 0x1 : 0x0));
    }

    /**
     * Writes an NBT tag. We need to wrap this in a NamedTag, as the named tag is not written itself.
     */
    public void writeNbt(SpecificTag nbt) {
        try {
            new NamedTag("", nbt).write(new DataOutputStream(new OutputStream() {
                @Override
                public void write(int b) {
                    bytes.insert((byte) b);
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
