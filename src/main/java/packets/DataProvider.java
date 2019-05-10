package packets;

import game.Game;
import proxy.CompressionManager;

import javax.xml.crypto.Data;
import javax.xml.ws.Provider;

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

        final int[] pos = {0};

        byte[] finalFullPacket = fullPacket;
        return new DataTypeProvider() {
            @Override
            public byte[] readByteArray(int size) {
                byte[] res = new byte[size];

                System.arraycopy(finalFullPacket, pos[0], res, 0, size);
                pos[0] += size;

                return res;
            }

            @Override
            public long readVarLong() {
                int numRead = 0;
                long result = 0;
                byte read;
                do {
                    if (!hasNext()) {
                        throw new RuntimeException("VarLong lacks bytes! We may be out of sync now.");
                    }
                    read = readNext();
                    int value = (read & 0b01111111);
                    result |= (value << (7 * numRead));

                    numRead++;
                    if (numRead > 10) {
                        throw new RuntimeException("VarLong is too big");
                    }
                } while ((read & 0b10000000) != 0);

                return result;
            }

            @Override
            public int readVarInt() {
                return DataReader.readVarInt(this::hasNext, this::readNext);
            }

            @Override
            public String readString() {
                int stringSize = readVarInt();

                StringBuilder sb = new StringBuilder();
                while (stringSize-- > 0) {
                    sb.appendCodePoint(readNext());
                }
                return sb.toString();
            }

            @Override
            public void skip(int amount) {
                while (amount-- > 0) {
                    readNext();
                }
            }

            @Override
            public int readShort() {
                byte low = readNext();
                byte high = readNext();
                return (((low & 0xFF) << 8) | (high & 0xFF));
            }

            @Override
            public byte readNext() {
                return finalFullPacket[pos[0]++];
            }

            @Override
            public boolean hasNext() {
                return pos[0] < finalFullPacket.length;
            }
        };
    }
}
