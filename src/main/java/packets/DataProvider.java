package packets;

import proxy.CompressionManager;

import javax.naming.SizeLimitExceededException;

public class DataProvider {
    private static final int MAX_SIZE = 2097152;
    private DataReader reader;
    private CompressionManager compressionManager;

    public DataProvider(DataReader reader) {
        this.reader = reader;
    }

    public void setCompressionManager(CompressionManager compressionManager) {
        this.compressionManager = compressionManager;
    }

    /**
     * Provides the object with all the bytes from the packet, allowing them to be read into the correct data types
     * easily. This method will also decompress the packet, as this is the first time we have the full packet
     * available, which is what we need for valid decompression.
     * @param size the packet size
     * @return the data parser for the decompressed packet
     */
    public DataTypeProvider withSize(int size) throws SizeLimitExceededException {
        byte[] compressed = reader.readByteArray(size);

        byte[] fullPacket;
        if (compressionManager.isCompressionEnabled()) {
            final int[] compressionPos = {0};
            int uncompressedSize = DataReader.readVarInt(
                () -> compressionPos[0] < compressed.length,
                () -> compressed[compressionPos[0]++]
            );

            // packets over this size will crash the game client, so it may help to reject them here
            if (uncompressedSize > MAX_SIZE) {
                throw new SizeLimitExceededException("WARNING: discarding packet over maximum size (size: " + uncompressedSize + ")");
            }

            fullPacket = compressionManager.decompressPacket(compressed, compressionPos[0], uncompressedSize);
        } else {
            fullPacket = compressed;
        }

        return DataTypeProvider.ofPacket(fullPacket);
    }
}
