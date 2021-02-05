package packets.builder;

import packets.UUID;
import packets.lib.ByteQueue;
import proxy.CompressionManager;
import se.llbit.nbt.SpecificTag;

import java.util.ArrayList;
import java.util.List;

public class DebugPacketBuilder extends PacketBuilder {
    List<String> parts = new ArrayList<>();
    boolean doPrint = true;

    public DebugPacketBuilder(int packetId) {
        super(packetId);
    }

    public DebugPacketBuilder() {
        super();
    }

    @Override
    public byte[] toArray() {
        return super.toArray();
    }

    @Override
    public void writeVarInt(int value) {
        super.writeVarInt(value);

        add("VarInt(" + value + ")");
    }

    @Override
    public void writeString(String str) {
        super.writeString(str);

        add("String(" + str.length() + ")");
    }

    @Override
    public void writeByteArray(byte[] arr) {
        super.writeByteArray(arr);

        add("ByteArr[" + arr.length + "]");
    }

    @Override
    public ByteQueue build() {
        System.out.println("Packet[" +String.join(" ", parts) + "]");
        return super.build();

    }

    @Override
    public ByteQueue build(CompressionManager compressionManager) {
        return super.build(compressionManager);
    }

    @Override
    public void writeShort(int shortVal) {
        super.writeShort(shortVal);

        add("Short(" + shortVal + ")");
    }

    @Override
    public void writeInt(int val) {
        add("Int[" + val + "](");
        super.writeInt(val);
        add(")");
    }

    @Override
    public void writeBoolean(boolean val) {
        super.writeBoolean(val);

        add("Bool(" + val + ")");
    }

    @Override
    public void writeNbt(SpecificTag nbt) {
        super.writeNbt(nbt);

        add("Nbt(" + nbt.getClass().getSimpleName() + ")");
    }

    @Override
    public void writeByte(byte b) {
        super.writeByte(b);

        add("Byte(" + b + ")");
    }

    @Override
    public void writeUUID(UUID uuid) {
        add("UUID(");
        super.writeUUID(uuid);
        add(")");
    }

    @Override
    public void writeLong(long val) {
        super.writeLong(val);
        add("Long(" + val + ")");
    }

    @Override
    public void writeVarIntArray(int[] arr) {
        add("VarIntArr[" + arr.length + "]");
        doPrint = false;
        super.writeVarIntArray(arr);
        doPrint = true;
    }

    @Override
    public void writeLongArray(long[] arr) {
        add("LongArr[" + arr.length + "]");
        doPrint = false;
        super.writeLongArray(arr);
        doPrint = true;
    }

    @Override
    public void writeStringArray(String[] arr) {
        add("StringArr[" + arr.length + "]");
        doPrint = false;
        super.writeStringArray(arr);
        doPrint = true;
    }
    
    private void add(String part) {
        if (doPrint) {
            parts.add(part);
        }
    }
}
