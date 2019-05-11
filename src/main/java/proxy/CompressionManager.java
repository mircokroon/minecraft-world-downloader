package proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

public class CompressionManager {
    int limit = 0;
    boolean compressionEnabled = false;

    public void enableCompression(int limit) {
        System.out.println("Enabled compression");
        this.limit = limit;
        compressionEnabled = true;
    }



    public byte[] decompress(byte[] input, int offset, int len) {
        if (!compressionEnabled) {
            return input;
        }
        if (len == 0) {
            byte[] res = new byte[input.length - offset];
            System.arraycopy(input, offset, res, 0, res.length);
            return res;
        }

        InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(
            input,
            offset,
            input.length - offset
        ));

        try {
            return inflater.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not decompress");
        }
        return new byte[0];
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void reset() {
        compressionEnabled = false;
    }
}
