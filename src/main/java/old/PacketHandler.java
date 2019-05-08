package old;

public class PacketHandler implements IPacketHandler {
    boolean compressionEnabled = false;
    int compressionThresshold = 0;
    int limit = 30;

    String type;

    public PacketHandler(String type) {
        this.type = type;
    }

    public void handle(byte[] packet) {


        //old.ByteArrayReader reader = new old.ByteArrayReader(packet);

        if (limit > 0) {
            limit++;
            //System.out.print(type + " (" + length + "):");
            //printArr(packet);
        }

        if (compressionEnabled) {
     //       handleCompressedPacket(reader);
        } else {
      //      handleUncompressedPakcet(reader);
        }


    }
/*
    private static void printArr(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : arr) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString());
    }

    private void handleCompressedPacket(old.ByteArrayReader packet) {
        //old.ByteArrayReader compressed = new old.ByteArrayReader(packet);
        //System.out.println("Packet length: " + readVarInt(compressed) + " Data length: " + readVarInt(compressed));
        int dataLength = readVarInt(packet);

        if (dataLength == 0) {
            System.out.println("Packet is NOT compressed! ID: " + Integer.toHexString(readVarInt(packet)));
        } else {
            old.ByteArrayReader decompressed = decompress(packet.getRemaining());
            System.out.println("UNCOMPRESSED! ID: " + Integer.toHexString(readVarInt(decompressed)));
        }

    }

    private void handleUncompressedPakcet(old.ByteArrayReader packet) {
        int packetID = readVarInt(packet);
        //System.out.println("Uncompressd packet found! ID: " + Integer.toHexString(packetID));
        if (type == "client" && packetID == 0x00) {
            System.out.print("HANDSHAKE: ");
            int version = readVarInt(packet);
            System.out.print(new String(packet.read(255)));
            System.out.print(":" + Arrays.toString(packet.read(2)) + ": ");
            int nextState = readVarInt(packet);

            System.out.println("v" + version + " :: next state = " + nextState);
        }

        if (packetID == 0x03) {
            System.out.println("COMPRESSION PACKET!!!");
            printArr(packet.getRemaining());
            compressionEnabled = true;
            compressionThresshold = readVarInt(packet);
        }
    }


    private old.ByteArrayReader decompress(byte[] compressed) {
        try {
            return new old.ByteArrayReader(IOUtils.toByteArray(new InflaterInputStream(new ByteArrayInputStream(compressed))));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
*/
    /*public byte[] decompress(byte[] bytesToDecompress) {
        byte[] returnValues = null;

        Inflater inflater = new Inflater();

        int numberOfBytesToDecompress = bytesToDecompress.length;

        inflater.setInput(bytesToDecompress,0, numberOfBytesToDecompress);

        int bufferSizeInBytes = numberOfBytesToDecompress;

        List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();

        try {
            while (!inflater.needsInput()) {
                byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];

                int numberOfBytesDecompressedThisTime = inflater.inflate(bytesDecompressedBuffer);

                for (int b = 0; b < numberOfBytesDecompressedThisTime; b++) {
                    bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
                }
            }

            returnValues = new byte[bytesDecompressedSoFar.size()];
            for (int b = 0; b < returnValues.length; b++) {
                returnValues[b] = bytesDecompressedSoFar.get(b);
            }

        } catch (DataFormatException dfe) {
            dfe.printStackTrace();
        }

        inflater.end();

        return returnValues;
    }*/
}
