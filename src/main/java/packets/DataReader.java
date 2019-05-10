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
    private int nextPacketSize = -1;

    private EncryptionManager encryptionManager;
    private UnaryOperator<byte[]> decrypt;
    private ByteConsumer transmit;


    public static DataReader clientBound(EncryptionManager manager) {
        return new DataReader(manager, manager::clientBoundDecrypt, manager::streamToClient);
    }

    public static DataReader serverBound(EncryptionManager manager) {
        return new DataReader(manager, manager::serverBoundDecrypt, manager::streamToServer);
    }

    private DataReader(EncryptionManager manager, UnaryOperator<byte[]> decrypt, ByteConsumer transmit) {
        queue = new LinkedList<>();
        currentPacket = new LinkedList<>();
        encryptedQueue = new LinkedList<>();

        this.encryptionManager = manager;
        this.decrypt = decrypt;
        this.transmit = transmit;
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(new DataProvider(this));
    }

    public PacketBuilder getBuilder() {
        return builder;
    }

    public void pushData(byte[] b, int amount) throws IOException  {
        if (amount == 0) { return; }


        if (encryptionManager.isEncryptionEnabled()) {
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
        } else {
            for (int i = 0; i < amount; i++) {
                queue.add(b[i]);
            }
        }

        do {
            if (nextPacketSize == -1 && hasBytes(1)) {
                nextPacketSize = readVarInt();
            }

            if (nextPacketSize > -1 && hasBytes(nextPacketSize)) {
                boolean forwardPacket = getBuilder().build(nextPacketSize);

                int expectedLength = nextPacketSize + varIntLength(nextPacketSize);
                if (currentPacket.size() != expectedLength) {
                    System.out.println("WARNING: packet parsing may have been incorrect! Expected length: " + expectedLength + ". Used bytes: " + currentPacket.size());
                }

                if (forwardPacket) {
                    transmit.consume(currentPacket);
                }
                currentPacket.clear();
                nextPacketSize = -1;
            }
        } while(hasBytes(1) && nextPacketSize == -1);
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
       int numRead = 0;
       int result = 0;
       byte read;
       do {
           if (!hasNext.get()) {
               throw new RuntimeException("VarInt lacks bytes! We may be out of sync now.");
           }
           read = readNext.get();
           int value = (read & 0b01111111);
           result |= (value << (7 * numRead));

           numRead++;
           if (numRead > 5) {
               throw new RuntimeException("VarInt is too big");
           }
       }
       while ((read & 0b10000000) != 0);

       return result;

   }

    public static int varIntLength(int value) {
        int numButes = 0;
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            numButes++;
        } while (value != 0);
        return numButes;
    }

    // From https://wiki.vg/Protocol#Packet_format
    public int readVarInt() {
        return readVarInt(this::hasNext, this::readNext);
    }


    public byte[] readByteArray(int arrayLength) {
        byte[] bytes = new byte[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            bytes[i] = readNext();
        }
        return bytes;
    }
}
