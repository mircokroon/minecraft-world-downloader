package packets;

import game.Game;
import packets.builder.PacketBuilder;
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


    /**
     * Initialise the reader. Gets a decryptor operator and transmitter method.
     * @param decrypt  the decryptor operator
     * @param transmit the transmit function
     */
    private DataReader(UnaryOperator<byte[]> decrypt, ByteConsumer transmit) {
        this.encryptionManager = Game.getEncryptionManager();
        this.decrypt = decrypt;
        this.transmit = transmit;

        reset();
    }

    /**
     * Reset the reader in case the connection was lost.
     */
    public void reset() {
        queue = new LinkedList<>();
        currentPacket = new LinkedList<>();
        encryptedQueue = new LinkedList<>();
        varIntPacketSize = new VarIntResult();
    }

    /**
     * Initialise a client-bound data reader.
     */
    public static DataReader clientBound(EncryptionManager manager) {
        return new DataReader(manager::clientBoundDecrypt, manager::streamToClient);
    }

    /**
     * Initialise a server-bound data reader.
     */
    public static DataReader serverBound(EncryptionManager manager) {
        return new DataReader(manager::serverBoundDecrypt, manager::streamToServer);
    }

    /**
     * Read a var int, uses the given hasNext and readNext methods to get the required bytes.
     */
    public static int readVarInt(Supplier<Boolean> hasNext, Supplier<Byte> readNext) {
        VarIntResult res = readVarInt(hasNext, readNext, new VarIntResult(false, 0, 0));
        if (!res.isComplete()) {
            throw new RuntimeException("VarInt lacks bytes! We may be out of sync now.");
        }
        return res.getResult();
    }

    /**
     * Read a full or partial varInt from the given reader method. As the connection will sometimes give us partial
     * varInts (with the rest having not yet arrived) we need to make sure we can handle partial results without the
     * connection becoming desynchronised. Adjusted from: https://wiki.vg/Protocol#Packet_format
     * @param res the object to the store the full or partial result in
     * @return the same object that it was given
     */
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

    /**
     * Push data to this reader.
     * @param b      the bytes array containing the new data
     * @param amount the number of bytes to read from the array
     */
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

    /**
     * If the packet is encrypted, decrypt it. Adds the decrypted bytes to the regular queue.
     * @param b      the encrypted bytes
     * @param amount the number of bytes to read from the given array
     */
    private void decryptPacket(byte[] b, int amount) {
        for (int i = 0; i < amount; i++) {
            encryptedQueue.add(b[i]);
        }

        // provide encryption in fixed size blocks, otherwise the decryptor will get angry.
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

    /**
     * Read packets from the byte queue. This method will first try to read a varInt indicating the upcoming packet's
     * size. Then, when the varInt is complete (may take several data transmissions), it will check if there is enough
     * bytes to complete the packet (this too may take several transmissions). After the packet is complete it will
     * be passed to the packet builder which may decompress and read the data.
     * <p>
     * If the packet builder returns true, this means we will forward the packet. If the builder returns false, we will
     * dump the packet and move on. This will happen for the encryption related packets as sending the real one to the
     * server will prevent us from getting the encryption keys.
     */
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

    /**
     * Check if we have more bytes in the queue right now.
     */
    private boolean hasNext() {
        return !queue.isEmpty();
    }

    /**
     * Read the packet size. Will continue reading with a previous result if that was not yet complete.
     */
    private VarIntResult readPacketSize() {
        if (!varIntPacketSize.isComplete()) {
            readVarInt(this::hasNext, this::readNext, varIntPacketSize);
        }
        return varIntPacketSize;
    }

    /**
     * Check if we have the given number of bytes. Used to check if the next package is complete.
     * @param amount the number of bytes required
     * @return true if we have sufficient bytes, otherwise false
     */
    private boolean hasBytes(int amount) {
        return amount <= queue.size();
    }


    private PacketBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(new DataProvider(this));
    }

    /**
     * Read a byte, also add it the current packet.
     */
    private byte readNext() {
        currentPacket.add(queue.peek());

        return queue.remove();
    }

    /**
     * Read an array of bytes from the queue. Used to get all the bytes for a packet.
     * @param arrayLength the number of bytes to get
     * @return the full array
     */
    byte[] readByteArray(int arrayLength) {
        byte[] bytes = new byte[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            bytes[i] = readNext();
        }
        return bytes;
    }
}
