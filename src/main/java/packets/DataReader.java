package packets;

import proxy.ByteConsumer;
import proxy.EncryptionManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class DataReader {
    private Queue<Byte> queue;
    private Queue<Byte> encryptedQueue;
    private Queue<Byte> currentPacket;
    private PacketBuilder builder;

    private EncryptionManager encryptionManager;
    private UnaryOperator<byte[]> decrypt;
    private ByteConsumer transmit;

    private VarIntResult varIntPacketSize;


    public static DataReader clientBound(EncryptionManager manager) {
        return new DataReader(manager, manager::clientBoundDecrypt, manager::streamToClient);
    }

    public static DataReader serverBound(EncryptionManager manager) {
        return new DataReader(manager, manager::serverBoundDecrypt, manager::streamToServer);
    }

    private DataReader(EncryptionManager manager, UnaryOperator<byte[]> decrypt, ByteConsumer transmit) {
        this.encryptionManager = manager;
        this.decrypt = decrypt;
        this.transmit = transmit;

        reset();
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(new DataProvider(this));
    }

    public PacketBuilder getBuilder() {
        return builder;
    }

    public void pushData(byte[] b, int amount) throws IOException {
        if (amount == 0) { return; }

        if (encryptionManager.isEncryptionEnabled()) {
            decryptPacket(b, amount);
        } else {
            for (int i = 0; i < amount; i++) {
                queue.add(b[i]);
            }
        }

        readPackets();
    }

    private void decryptPacket(byte[] b, int amount) {
        for (int i = 0; i < amount; i++) {
            encryptedQueue.add(b[i]);
        }

        if (encryptedQueue.size() >= encryptionManager.blockSize) {
            int toEncrypt = encryptedQueue.size() - (encryptedQueue.size() % encryptionManager.blockSize);
            byte[] encrypted = new byte[toEncrypt];
            for (int i = 0; i < toEncrypt; i++) {
                encrypted[i] = encryptedQueue.remove();
            }

            byte[] decrypted = decrypt.apply(encrypted);
            for (byte aDecrypted : decrypted) {
                queue.add(aDecrypted);
            }
        }
    }

    private void readPackets() throws IOException {
        int nextPacketSize;
        while (hasNext() && readPacketSize().isComplete()) {
            nextPacketSize = varIntPacketSize.getResult();

            // if we have enough bytes to parse the packet
            if (!hasBytes(nextPacketSize)) {
                return;
            }
            // parse the packet (including decompression)
            boolean forwardPacket = getBuilder().build(nextPacketSize);

            // check if the the packet length was correct
            int expectedLength = nextPacketSize + varIntPacketSize.numBytes();
            if (currentPacket.size() != expectedLength) {
                System.out.println("WARNING: packet parsing may have been incorrect! Expected length: " + expectedLength + ". Used bytes: " + currentPacket.size());
            }

            // forward the packet unless the packet builder decided swallowed it
            if (forwardPacket) {
                transmit.consume(currentPacket);
            }

            // clean up to prepare for next packet
            currentPacket.clear();
            varIntPacketSize.reset();
        }
    }

    private byte readNext() {
        currentPacket.add(queue.peek());

        return queue.remove();
    }

    private boolean hasNext() {
        return !queue.isEmpty();
    }

    private boolean hasBytes(int amount) {
        return amount <= queue.size();
    }

    public static int readVarInt(Supplier<Boolean> hasNext, Supplier<Byte> readNext) {
        VarIntResult res = readVarInt(hasNext, readNext, new VarIntResult(false, 0, 0));
        if (!res.isComplete()) {
            throw new RuntimeException("VarInt lacks bytes! We may be out of sync now.");
        }
        return res.getResult();
    }

    // From https://wiki.vg/Protocol#Packet_format
    public static VarIntResult readVarInt(Supplier<Boolean> hasNext, Supplier<Byte> readNext, VarIntResult res) {
        byte read;
        do {
            if (!hasNext.get()) {
                return res;
            }
            read = readNext.get();
            int value = (read & 0b01111111);
            res.addValue(value << (7 * res.numBytes()));

            res.addByteRead();
            if (res.numBytes() > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        }
        while ((read & 0b10000000) != 0);

        res.setComplete(true);
        return res;
    }

    public static int varIntLength(int value) {
        int numButes = 0;
        do {
            byte temp = (byte) (value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            numButes++;
        } while (value != 0);
        return numButes;
    }

    public VarIntResult readPacketSize() {
        if (!varIntPacketSize.isComplete()) {
            readVarInt(this::hasNext, this::readNext, varIntPacketSize);
        }
        return varIntPacketSize;
    }


    public byte[] readByteArray(int arrayLength) {
        byte[] bytes = new byte[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            bytes[i] = readNext();
        }
        return bytes;
    }

    public void reset() {
        queue = new LinkedList<>();
        currentPacket = new LinkedList<>();
        encryptedQueue = new LinkedList<>();
        varIntPacketSize = new VarIntResult();
    }
}
