package packets;

import java.util.LinkedList;
import java.util.Queue;

public class DataReader {
    Queue<Byte> queue;
    int pos;
    PacketBuilder builder;
    int nextPacketSize = -1;
    int readCalledSince = 0;

    public DataReader() {
        queue = new LinkedList<>();
        pos = 0;
    }

    public void setBuilder(PacketBuilder builder) {
        this.builder = builder;
        builder.setReader(this);
    }

    public PacketBuilder getBuilder() {
        return builder;
    }

    public void pushData(byte[] b, int amount) {
        if (amount == 0) { return; }

        for (int i = 0; i < amount; i++) {
            queue.add(b[i]);
        }

        do {
            //System.out.println(queue.stream().map(el -> el.length).collect(Collectors.toList()));
            if (nextPacketSize == -1 && hasBytes(1)) {
                nextPacketSize = readVarInt();
            }

            readCalledSince = 0;
            if (nextPacketSize > -1 && hasBytes(nextPacketSize)) {
                System.out.println("(" + pos + ") Bytes: " + (nextPacketSize) + " :: enough for " + nextPacketSize);
                getBuilder().build(nextPacketSize);

                if (readCalledSince != nextPacketSize) {
                    System.out.println("WARNING: packet parsing may have been incorrect! Expected length: " + nextPacketSize + ". Used bytes: " + readCalledSince);
                }

                nextPacketSize = -1;
            }
        } while(hasBytes(5) && nextPacketSize == -1);
    }

   private byte readNext() {
        readCalledSince++;

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
