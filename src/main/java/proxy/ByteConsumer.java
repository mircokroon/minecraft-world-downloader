package proxy;

import java.io.IOException;
import java.util.Queue;

public interface ByteConsumer {
    void consume(Queue<Byte> arr) throws IOException;
}
