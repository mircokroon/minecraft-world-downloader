package game.data.chunk;

import config.Config;
import game.data.coordinates.CoordinateDim2D;
import game.data.region.McaFile;
import proxy.CompressionManager;
import se.llbit.nbt.NamedTag;
import util.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Binary (raw NBT) version of a chunk.
 */
public class ChunkBinary implements Serializable {
    private static final long serialVersionUID = 4723655497132229129L;

    private final static byte COMPRESSION_TYPE = 2;
    private int timestamp;
    private int location;
    private int size;
    private byte[] chunkData;
    private ChunkBinary() {
        this.timestamp = 0;
        this.location = -1;
    }

    public ChunkBinary(int timestamp, int location, int size, byte[] chunkData) {
        this.timestamp = timestamp;
        this.location = location;
        this.size = size;
        this.chunkData = chunkData;
    }

    /**
     * Convert a chunk to a ChunkBinary object.
     * @param chunk the chunk
     * @return the binary version of the chunk
     */
    public static ChunkBinary fromChunk(Chunk chunk) throws IOException {
        NamedTag nbt = chunk.toNbt();

        // this only happens for empty chunks (hopefully)
        if (nbt == null) {
            return null;
        }

        // debug option - write all chunks nbt as text to files so it can be easily verified
        if (Config.writeChunksAsNbt()) {
            String filename = chunk.location.getX() + "_" + chunk.location.getZ();

            Path output = PathUtils.toPath(Config.getWorldOutputDir(), "debug", filename);
            Files.createDirectories(output.getParent());
            Files.write(output, Collections.singleton(nbt.tag.toString()));
        }

        ChunkBinary binary = new ChunkBinary();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            nbt.write(new DataOutputStream(output));
        } catch (Exception ex) {
            System.out.println("Unable to write chunk " + chunk.location.getX() + ", " + chunk.location.getZ());
            ex.printStackTrace();
            return null;
        }

        byte[] data = CompressionManager.zlibCompress(output.toByteArray());

        byte[] finalData = new byte[data.length + 5];
        int lengthToWrite = data.length + 1;
        finalData[0] = (byte) (lengthToWrite >>> 24);
        finalData[1] = (byte) (lengthToWrite >>> 16);
        finalData[2] = (byte) (lengthToWrite >>> 8);
        finalData[3] = (byte) (lengthToWrite);
        finalData[4] = COMPRESSION_TYPE;

        System.arraycopy(data, 0, finalData, 5, data.length);

        int fullsize = finalData.length + 5;
        binary.size = fullsize / McaFile.SECTOR_SIZE + (fullsize % McaFile.SECTOR_SIZE == 0 ? 0 : 1);

        binary.chunkData = finalData;
        binary.setTimestamp((int) (System.currentTimeMillis() / 1000));

        return binary;
    }

    public Chunk toChunk(CoordinateDim2D coordinate2D) {
        int length = (chunkData[0] & 0xFF) << 24 | (chunkData[1] & 0xFF) << 16 | (chunkData[2] & 0xFF) << 8 | (chunkData[3] & 0xFF);

        byte[] compressedNbtData = new byte[length - 1];
        System.arraycopy(this.chunkData, 5, compressedNbtData, 0, length - 1);

        byte[] data = CompressionManager.zlibDecompress(compressedNbtData);
        NamedTag nbt = (NamedTag) NamedTag.read(new DataInputStream(new ByteArrayInputStream(data)));

        return ChunkFactory.getInstance().fromNbt(nbt, coordinate2D);
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getChunkData() {
        return chunkData;
    }

    public void setChunkData(byte[] chunkData) {
        this.chunkData = chunkData;
    }
}
