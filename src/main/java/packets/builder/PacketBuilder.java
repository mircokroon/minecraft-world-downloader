package packets.builder;

import packets.lib.ByteQueue;

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

}
