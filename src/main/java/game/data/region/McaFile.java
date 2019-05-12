package game.data.region;

import game.Game;
import game.data.Coordinate2D;
import game.data.chunk.ChunkBinary;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class McaFile {
    public final static int SECTOR_SIZE = 4096;
    Map<Integer, ChunkBinary> chunkMap;
    Path filePath;
    byte[] input;
    Coordinate2D regionLocation;

    /**
     * Class to parse and write MCA files
     * @param file the MCA file to be used
     * @throws IOException
     */
    public McaFile(File file) throws IOException {
        chunkMap = readFile(file);
        filePath = Paths.get(file.getAbsolutePath());
        String[] bits = file.getName().split("\\.");
        regionLocation = new Coordinate2D(Integer.parseInt(bits[1]), Integer.parseInt(bits[2]));
    }

    public McaFile(Coordinate2D pos, Map<Integer, ChunkBinary> chunkMap) {
        regionLocation = pos;
        Path filePath = Paths.get(Game.getExportDirectory(), "region", "r." + pos.getX() + "." + pos.getZ() + ".mca");

        this.chunkMap = new HashMap<>();
        if (filePath.toFile().exists()) {
            try {
                this.chunkMap = readFile(filePath.toFile());
            } catch (IOException e) {
                // fail silently, we will just overwrite the file instead
            }
        }

        chunkMap.forEach((key, value) -> this.chunkMap.put(key, value));
        this.filePath = filePath;
    }

    /**
     * Write the MCA file to the given path. Should be called after merge.
     * @throws IOException
     */
    public void write() throws IOException {
        byte[] locations = new byte[SECTOR_SIZE];
        byte[] timestamps = new byte[SECTOR_SIZE];
        Map<Integer, byte[]> chunkDataList = new HashMap<>();
        final int[] maxpos = {0};

        updateChunkLocations(chunkMap);

        chunkMap.forEach((pos, chunk) -> {
            setLocation(locations, pos, chunk);
            setTimestamp(timestamps, pos, chunk);
            setChunkData(chunkDataList, chunk);

            int bytePosition = (chunk.getSize() + chunk.getLocation() - 2) * SECTOR_SIZE;
            if (bytePosition > maxpos[0]) {
                maxpos[0] = bytePosition;
            }
        });

        byte[] toWrite = join(locations, timestamps, chunkDataList, maxpos[0]);
        Files.write(filePath, toWrite);
    }

    /**
     * Update the chunk positions in the chunkdata section of the file. Different files may have
     * have overlapping locations even if the coordinates are different, so we need to recompute
     * the locations before saving.
     * @param chunkMap the chunkmap to set the positions for
     */
    private void updateChunkLocations(Map<Integer, ChunkBinary> chunkMap) {
        AtomicInteger currentAddress = new AtomicInteger(2);
        chunkMap.forEach((pos, chunk) -> {
            chunk.setLocation(currentAddress.get());
            currentAddress.addAndGet(chunk.getSize());
        });
    }

    private void setChunkData(Map<Integer, byte[]> chunkDataList, ChunkBinary chunk) {
        chunkDataList.put(chunk.getLocation(), chunk.getChunkData());
    }

    private void setTimestamp(byte[] timestamp, int pos, ChunkBinary chunk) {
        timestamp[pos] = (byte) (chunk.getTimestamp() >>> 24);
        timestamp[pos+1] = (byte) (chunk.getTimestamp() >>> 16);
        timestamp[pos+2] = (byte) (chunk.getTimestamp() >>> 8);
        timestamp[pos+3] = (byte) chunk.getTimestamp();
    }

    private void setLocation(byte[] locations, Integer pos, ChunkBinary chunk) {
        locations[pos] = (byte) (chunk.getLocation() >>> 16);
        locations[pos+1] = (byte) (chunk.getLocation() >>> 8);
        locations[pos+2] = (byte) chunk.getLocation();
        locations[pos+3] = (byte) chunk.getSize();
    }

    /**
     * Join the various parts of the byte array to be saved into one.
     * @param maxpos the largest byte sector address we will need, depending on the amount of data we
     *               are saving.
     * @return the final byte array
     */
    private byte[] join(byte[] locations, byte[] timestamps, Map<Integer, byte[]> datalist, int maxpos) {
        int totalBytes = locations.length + timestamps.length + maxpos;

        byte[] res = new byte[totalBytes];
        System.arraycopy(locations, 0, res, 0, locations.length);
        System.arraycopy(timestamps, 0, res, SECTOR_SIZE, timestamps.length);

        datalist.forEach((i, data) -> {
            int pos = i * SECTOR_SIZE;
            System.arraycopy(data, 0, res, pos, data.length);
        });

        return res;
    }

    /**
     * Convert the MCA file into individual chunk data.
     * For details on the MCA file format: https://minecraft.gamepedia.com/Region_file_format
     */
    private Map<Integer, ChunkBinary> readFile(File mca) throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(mca));

        byte[] locations = Arrays.copyOfRange(bytes, 0, SECTOR_SIZE);
        byte[] timestamps = Arrays.copyOfRange(bytes, SECTOR_SIZE, SECTOR_SIZE *2);
        byte[] chunkDataArray = Arrays.copyOfRange(bytes, SECTOR_SIZE *2, bytes.length);

        HashMap<Integer, ChunkBinary> chunkMap = new HashMap<>();

        for (int i = 0; i < locations.length; i += 4) {
            int timestamp = bytesToInt(timestamps, i, i+3);
            int location = bytesToInt(locations, i, i+2);
            int size = locations[i+3] & 0xFF;

            if (size == 0) { continue; }

            // chunk location includes first location/timestamp sections so we need to lower the addresses by 2 sectors
            int chunkDataStart = (location - 2) * SECTOR_SIZE;
            int chunkDataEnd = (location + size - 2) * SECTOR_SIZE;

            byte[] chunkData = Arrays.copyOfRange(chunkDataArray, chunkDataStart,  chunkDataEnd);

            // i is the unique identifier of this chunk within the file, based on coordinates thus consistent
            chunkMap.put(i, new ChunkBinary(timestamp, location, size, chunkData));
        }

        this.input = bytes;
        return chunkMap;
    }

    /**
     * Converts a number of bytes to a big-endian int.
     * @param arr the total array of bytes
     * @param start the first byte to use (inclusive)
     * @param end the last byte to use (INCLUSIVE)
     * @return the integer created from the bytes
     */
    private static int bytesToInt(byte[] arr, int start, int end) {
        int res = 0;
        do {
            res |= (arr[start] & 0xFF) << (end - start) * 8;
        } while(start++ < end);

        return res;
    }

    // 4 * ((x & 31) + (z & 31) * 32)
    public List<Coordinate2D> getChunkPositions() {
        return chunkMap.keySet().stream().map(el -> {
            int offset = el / 4;
            int localX = offset & 0x1F;
            int localZ = offset >>> 5 ;

            return new Coordinate2D(regionLocation.getX() * 32 + localX, regionLocation.getZ() * 32 + localZ);
        }).collect(Collectors.toList());
    }
}
