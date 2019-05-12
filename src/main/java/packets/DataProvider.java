package packets;

import game.Game;
import proxy.CompressionManager;

public class DataProvider {
    private DataReader reader;
    private CompressionManager compressionManager;

    public DataProvider(DataReader reader) {
        this.reader = reader;
        this.compressionManager = Game.getCompressionManager();
    }

    /**
     * Provides the object with all the bytes from the packet, allowing them to be read into the correct data types
     * easily. This method will also decompress the packet, as this is the first time we have the full packet
     * available, which is what we need for valid decompression.
     * @param size the packet size
     * @return the data parser for the decompressed packet
     */
    public DataTypeProvider withSize(int size) {
        byte[] compressed = reader.readByteArray(size);

        byte[] fullPacket;
        if (compressionManager.isCompressionEnabled()) {
            final int[] compressionPos = {0};
            int uncompressedSize = DataReader.readVarInt(
                () -> compressionPos[0] < compressed.length,
                () -> compressed[compressionPos[0]++]
            );

            fullPacket = compressionManager.decompress(compressed, compressionPos[0], uncompressedSize);
        } else {
            fullPacket = compressed;
        }

        return new DataTypeProvider(fullPacket);
    }
}
