package packets;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class DataReader {
    Queue<byte[]> queue;
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

        byte[] toAdd = new byte[amount];
        System.arraycopy(b, 0, toAdd, 0, amount);
        queue.add(toAdd);

        System.out.println(queue.stream().map(el -> el.length).collect(Collectors.toList()));

        if (nextPacketSize == -1) {
            try {
                nextPacketSize = readVarInt();
            } catch(RuntimeException ex) {
                System.out.println("Not enough bytes to build packet yet");
            }
        }


        readCalledSince = 0;
        if (nextPacketSize > -1 && hasBytes(nextPacketSize)) {
            System.out.println("Bytes: " + nextPacketSize + " :: enough for " + nextPacketSize);
            getBuilder().build(nextPacketSize);

            if (readCalledSince != nextPacketSize) {
                System.out.println("WARNING: packet parsing may have been incorrect! Expected length: " + nextPacketSize + ". Used bytes: " + readCalledSince);
            }

            nextPacketSize = -1;
        }
    }

   private byte readNext() {
        readCalledSince++;

        if (queue.peek().length == pos) {
            System.out.println("REMOVING");
           queue.remove();
           pos = 0;
       }

        return queue.peek()[pos++];
   }

   private boolean hasNext() {
       if (queue.isEmpty()) return false;

       return queue.peek().length != pos || queue.size() > 1;
   }

   private boolean hasBytes(int amount) {
        int sum = -pos;
        for(byte[] bytes : queue) {
            sum += bytes.length;
            if (sum >= amount) {
                return true;
            }
        }
        return false;
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


    // TODO: optimize this to not be stupid
    public void skip(int size) {
        while (size-->0) {
            readNext();
        }
    }
}
