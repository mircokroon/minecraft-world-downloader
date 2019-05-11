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

        byte[] finalFullPacket = fullPacket;
        return new DataTypeProvider(finalFullPacket);
    }
}
