package proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

public class CompressionManager {
    int limit = 0;
    boolean compressionEnabled = false;

    public void enableCompression(int limit) {
        System.out.println("Compression active!");
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
        System.out.println("Decompressing " + input.length + " bytes to " + len + " bytes");
        InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(input, offset, input.length - offset));

        int nRead = 0;
        int readOffset = 0;
        byte[] data = new byte[len];

        try {
            while ((nRead = inflater.read(data, readOffset += nRead, data.length)) != -1) {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("WARNING: could not decompress stream!");
        }
        System.out.println("Succesfully decompressed: " + data.length);
        return data;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
}
