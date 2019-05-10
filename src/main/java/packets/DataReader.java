package packets;

import proxy.ByteConsumer;
import proxy.EncryptionManager;

import javax.xml.ws.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;

public class DataReader {
    Queue<Byte> queue;
    Queue<Byte> encryptedQueue;
    Queue<Byte> currentPacket;
    PacketBuilder builder;
    int nextPacketSize = -1;
    int readCalledSince = 0;

    public DataReader() {
        queue = new LinkedList<>();
        currentPacket = new LinkedList<>();
        encryptedQueue = new LinkedList<>();
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(new DataProvider(this));
    }

    public PacketBuilder getBuilder() {
        return builder;
    }

    public void pushData(byte[] b, int amount, EncryptionManager encryptionManager, ByteConsumer transmit) throws IOException  {
        if (amount == 0) { return; }


        if (encryptionManager.isEncryptionEnabled()) {
            // add all bytes to the encrypted queue
            for (int i = 0; i < amount; i++) {
                encryptedQueue.add(b[i]);
            }
            //System.out.println("Added " + amount + " to encryption queue : " + Arrays.toString(b));

            if (encryptedQueue.size() >= encryptionManager.blockSize) {
                int toEncrypt = encryptedQueue.size() - (encryptedQueue.size() % encryptionManager.blockSize);
                byte[] encrypted = new byte[toEncrypt];
                for (int i = 0; i < toEncrypt; i++) {
                    encrypted[i] = encryptedQueue.remove();
                }
                //System.out.println("Decrypting: " + encrypted.length + " / " + encryptedQueue.size() + " :: " + Arrays.toString(encrypted));
                byte[] decrypted = encryptionManager.decrypt(encrypted);
                //System.out.println("Succesfully decrypted! : " + decrypted.length );
                for (byte aDecrypted : decrypted) {
                    queue.add(aDecrypted);
                }
            }
        } else {
            //System.out.println("Not decrypting " + amount + " :: " + Arrays.toString(b));
            for (int i = 0; i < amount; i++) {
                queue.add(b[i]);
            }
        }

        do {
            if (nextPacketSize == -1 && hasBytes(1)) {
                nextPacketSize = readVarInt();
            }

            if (nextPacketSize > -1 && hasBytes(nextPacketSize)) {
                //System.out.println("Bytes: " + (nextPacketSize) + " :: enough for " + nextPacketSize);


                boolean forwardPacket = getBuilder().build(nextPacketSize);
                //System.out.println(currentPacket.size() + " // " + currentPacket);
                if (forwardPacket) {
                    transmit.consume(currentPacket);
                }
                if (currentPacket.size() != nextPacketSize) {
                    System.out.println("WARNING: packet parsing may have been incorrect! Expected length: " + nextPacketSize + ". Used bytes: " + currentPacket.size());
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
