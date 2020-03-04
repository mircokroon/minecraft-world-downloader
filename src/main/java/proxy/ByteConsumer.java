package proxy;

import packets.lib.ByteQueue;

import java.io.IOException;
import java.util.Queue;

public interface ByteConsumer {
    void consume(ByteQueue arr) throws IOException;
}
