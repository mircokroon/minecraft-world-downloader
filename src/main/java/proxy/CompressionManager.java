package proxy;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class CompressionManager {
    private int compressionLimit = 0;
    private boolean compressionEnabled = false;

    public void enableCompression(int limit) {
        this.compressionLimit = limit;
        compressionEnabled = true;
    }

    /**
     * Compresses the stream if the size is greater than the compression limit.
     * Source: https://dzone.com/articles/how-compress-and-uncompress
     */
    public static byte[] zlibCompress(byte[] input) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }

    public static byte[] zlibDecompress(byte[] input) {
        InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(input));

        try {
            return IOUtils.toByteArray(inflater);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not decompress");
        }
        return new byte[0];
    }

    // Source: https://stackoverflow.com/a/44922240
    public static byte[] gzipCompress(byte[] uncompressedData) {
        byte[] result = new byte[]{};
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(uncompressedData.length);
             GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(uncompressedData);
            gzipOS.close();
            result = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Source: https://stackoverflow.com/a/44922240
    public static byte[] gzipDecompress(byte[] compressedData) {
        byte[] result = new byte[]{};
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPInputStream gzipIS = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            result = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Compressed a packet if it's over the limit. Otherwise, the original is returned. Will also return
     * the original if something goes wrong during compression, but that will surely never happen.
     */
    public byte[] compressPacket(byte[] input) {
        if (!compressionEnabled || input.length <= compressionLimit) {
            return input;
        }

        try {
            return zlibCompress(input);
        } catch (IOException e) {
            e.printStackTrace();
            return input;
        }
    }

    /**
     * Decompress the given input.
     * @param input  the input data
     * @param offset the offset to start decompression from
     * @param len    the length of the compressed data. When 0, no decompression will be done.
     * @return the decompressed data
     */
    public byte[] decompressPacket(byte[] input, int offset, int len) {
        if (!compressionEnabled) {
            return input;
        }

        if (len == 0) {
            byte[] res = new byte[input.length - offset];
            System.arraycopy(input, offset, res, 0, res.length);
            return res;
        }

        byte[] toDecompress = new byte[input.length - offset];
        System.arraycopy(input, offset, toDecompress, 0, toDecompress.length);
        return zlibDecompress(toDecompress);
    }


    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void reset() {
        compressionEnabled = false;
    }
}
