package proxy;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import packets.builder.PacketBuilder;
import packets.lib.ByteQueue;

public class PacketInjector {
    private final ConcurrentLinkedQueue<ByteQueue> insertedPackets;
    private final CompressionManager compressionManager;

    public PacketInjector(CompressionManager compressionManager) {
        this.insertedPackets = new ConcurrentLinkedQueue<>();
        this.compressionManager = compressionManager;
    }

    /**
     * Adds a packet to the queue. This queue is checked whenever a packet is sent, and they will be sent to the
     * client after.
     */
    public void enqueuePacket(PacketBuilder packet) {
        insertedPackets.add(packet.build(compressionManager));
    }

    public void clear() {
        this.insertedPackets.clear();
    }

    public boolean hasNext() {
        return !insertedPackets.isEmpty();
    }

    public ByteQueue getNext() {
        return insertedPackets.remove();
    }

    public int size() {
        return insertedPackets.size();
    }
}
