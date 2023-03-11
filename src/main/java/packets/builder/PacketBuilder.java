package packets.builder;

import config.Config;
import config.Version;
import game.protocol.Protocol;
import packets.DataTypeProvider;
import packets.UUID;
import packets.lib.ByteQueue;
import proxy.CompressionManager;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

public class PacketBuilder {
    ByteQueue bytes;

    public PacketBuilder(int packetId) {
        this.bytes = new ByteQueue();
        writeVarInt(packetId);
    }

    public PacketBuilder(String id) {
        this(Config.versionReporter().getProtocol().clientBound(id));
    }

    public PacketBuilder() {
        this.bytes = new ByteQueue();
    }

    public byte[] toArray() {
        return bytes.toArray();
    }

    public void copy(DataTypeProvider provider, NetworkType... types) {
        for (NetworkType type : types) {
            type.copy(provider, this);
        }
    };


    public static PacketBuilder constructClientMessage(String message, MessageTarget target) {
        return constructClientMessage(new Chat(message), target);
    }
    /**
     * Construct a message packet for the client.
     */
    public static PacketBuilder constructClientMessage(Chat message, MessageTarget target) {
        Protocol protocol = Config.versionReporter().getProtocol();
        String packetName = Config.versionReporter().isAtLeast(Version.V1_19) ? "SystemChat" : "Chat";
        PacketBuilder builder = new PacketBuilder(protocol.clientBound(packetName));

        builder.writeString(message.toJson());
        builder.writeByte(target.getIdentifier());

        // uuid is only included from 1.16, from 1.19 player chat is a different packet
        if (Config.versionReporter().isAtLeast(Version.V1_16) && !Config.versionReporter().isAtLeast(Version.V1_19)) {
            builder.writeUUID(new UUID(0L, 0L));
        }
        return builder;
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

    private static ByteQueue createVarInt(int value) {
        ByteQueue res = new ByteQueue(5);
        writeVarInt(res, value);
        return res;
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
     * If we're building a packet and we are given a compressionManager, that means we need to compress the packet
     * if it's large enough. The compression manager does compressing, but we still need to prefix the size and let the
     * client know if it was actually compressed.
     */
    public ByteQueue build(CompressionManager compressionManager) {
        if (!compressionManager.isCompressionEnabled()) {
            return build();
        }

        byte[] original = bytes.toArray();
        byte[] compressed = compressionManager.compressPacket(original);

        // no compression happened
        if (compressed == original) {
            // without compression the prefix is packet length + 0 byte
            ByteQueue prefix = createVarInt(original.length + 1);
            prefix.insert((byte) 0);

            this.bytes.prepend(prefix);
        } else {
            // with compression we need to first prefix a varInt of the uncompressed data length
            ByteQueue dataLen = createVarInt(original.length);

            // ...and then prefix the length of the entire packet
            ByteQueue packetLen = createVarInt(dataLen.size() + compressed.length);

            byte[] res = new byte[compressed.length + dataLen.size() + packetLen.size()];
            System.arraycopy(packetLen.toArray(), 0, res, 0, packetLen.size());
            System.arraycopy(dataLen.toArray(), 0, res, packetLen.size(), dataLen.size());
            System.arraycopy(compressed, 0, res, packetLen.size() + dataLen.size(), compressed.length);

            return new ByteQueue(res);
        }
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


    public void writeByte(byte b) {
        bytes.insert(b);
    }

    public void writeUUID(UUID uuid) {
        writeLong(uuid.getLower());
        writeLong(uuid.getUpper());
    }

    public void writeLong(long val) {
        byte[] bytes = new byte[8];
        bytes[7] = (byte) (val & 0xFF);
        bytes[6] = (byte) (val >> 8 & 0xFF);
        bytes[5] = (byte) (val >> 16 & 0xFF);
        bytes[4] = (byte) (val >> 24 & 0xFF);
        bytes[3] = (byte) (val >> 32 & 0xFF);
        bytes[2] = (byte) (val >> 40 & 0xFF);
        bytes[1] = (byte) (val >> 48 & 0xFF);
        bytes[0] = (byte) (val >> 56 & 0xFF);

        writeByteArray(bytes);
    }

    public void writeVarIntArray(int[] arr) {
        for (int val : arr) {
            writeVarInt(val);
        }
    }

    public void writeLongArray(long[] arr) {
        for (long val : arr) {
            writeLong(val);
        }
    }

    public void writeStringArray(String[] arr) {
        for (String val : arr) {
            writeString(val);
        }
    }

    public void writeIntArray(int[] arr) {
        for (int i : arr) {
            writeInt(i);
        }
    }

    public void writeFloat(float val) {
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.putFloat(val);
        ((Buffer) buffer).flip();
        writeByteArray(buffer.array());
    }

    public void writeBitSet(BitSet bits) {
        long[] longs = bits.toLongArray();
        writeVarInt(longs.length);
        writeLongArray(longs);
    }


    public void copyRemainder(DataTypeProvider provider) {
        writeByteArray(provider.readByteArray(provider.remaining()));
    }
}
