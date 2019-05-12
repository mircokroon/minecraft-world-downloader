package proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

public class CompressionManager {
    private int compressionLimit = 0;
    private boolean compressionEnabled = false;

    public void enableCompression(int limit) {
        System.out.println("Enabled compression");
        this.compressionLimit = limit;
        compressionEnabled = true;
    }

    /**
     * Compresses the stream if the size is greater than the compression limit.
     * @param input the input to compress
     * @return the compressed input
     */
    public byte[] dompress(byte[] input) {
        throw new UnsupportedOperationException("Compression has not yet been implemented :(");
    }


    /**
     * Decompress the given input.
     * @param input  the input data
     * @param offset the offset to start decompression from
     * @param len    the length of the compressed data. When 0, no decompression will be done.
     * @return the decompressed data
     */
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
