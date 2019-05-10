package packets;

import proxy.ByteConsumer;
import proxy.EncryptionManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class DataReader {
    Queue<Byte> queue;
    Queue<Byte> currentPacket;
    PacketBuilder builder;
    int nextPacketSize = -1;
    int readCalledSince = 0;

    public DataReader() {
        queue = new LinkedList<>();
        currentPacket = new LinkedList<>();
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(this);
    }

    public PacketBuilder getBuilder() {
        return builder;
    }

    public void pushData(byte[] b, int amount, EncryptionManager encryptionManager, ByteConsumer transmit) throws IOException  {
        if (amount == 0) { return; }

        byte[] decrypted = encryptionManager.decrypt(b, amount);
        for (int i = 0; i < decrypted.length; i++) {
            queue.add(decrypted[i]);
        }

        do {
            if (nextPacketSize == -1 && hasBytes(1)) {
                nextPacketSize = readVarInt();
            }

            if (nextPacketSize > -1 && hasBytes(nextPacketSize)) {
                System.out.println("Bytes: " + (nextPacketSize) + " :: enough for " + nextPacketSize);


                boolean forwardPacket = getBuilder().build(nextPacketSize);
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



    // From https://wiki.vg/Protocol#Packet_format
    public int readVarInt() {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            if (!hasNext()) {
                throw new RuntimeException("VarInt lacks bytes! We may be out of sync now.");
            }
            read = readNext();
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


    public String readString() {
        int stringSize = readVarInt();

         StringBuilder sb = new StringBuilder();
         while (stringSize-- > 0) {
             sb.appendCodePoint(readNext());
         }
         return sb.toString();
    }

    public int readShort() {
        byte low = readNext();
        byte high = readNext();
        return (((low & 0xFF) << 8) | (high & 0xFF));
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


    // TODO: optimize this to not be stupid
    public void skip(int size) {
        while (size-->0) {
            readNext();
        }
    }

    public byte[] readByteArray(int arrayLength) {
        byte[] bytes = new byte[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            bytes[i] = readNext();
        }
        return bytes;
    }
}
