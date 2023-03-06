package game.data.region;

import config.Config;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import org.apache.commons.io.IOUtils;
import util.PathUtils;

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
    private HashMap<Integer, ChunkBinary> chunkMap;
    private final Path filePath;
    private final Coordinate2D regionLocation;
    private final boolean isEntityFile;


    /**
     * Parse MCA from a given file location.
     * @param file the MCA file to be used
     */
    public McaFile(File file, boolean isEntityFile) throws IOException {
        this.isEntityFile = isEntityFile;

        chunkMap = readFile(file);
        filePath = PathUtils.toPath(file.getAbsolutePath());
        String[] bits = file.getName().split("\\.");
        regionLocation = new Coordinate2D(Integer.parseInt(bits[1]), Integer.parseInt(bits[2]));
    }
    public McaFile(File file) throws IOException {
        this(file, false);
    }

    /**
     * Generate an MCA file from a given map of chunk binaries. This method will try to read this MCA file to merge with
     * it so that existing chunks are not deleted.
     * @param pos      the positon of this file
     */
    public McaFile(CoordinateDim2D pos, boolean isEntityFile) {
        this.isEntityFile = isEntityFile;
        regionLocation = pos.offsetRegion();

        String filename = "r." + regionLocation.getX() + "." + regionLocation.getZ() + ".mca";
        String directory = this.isEntityFile ? "entities" : "region";
        Path filePath = PathUtils.toPath(Config.getWorldOutputDir(), pos.getDimension().getPath(), directory, filename);

        this.chunkMap = new HashMap<>();
        if (filePath.toFile().exists()) {
            try {
                this.chunkMap = readFile(filePath.toFile());
            } catch (IOException e) {
                // fail silently, we will just overwrite the file instead
            }
        }

        this.filePath = filePath;
    }
    public McaFile(CoordinateDim2D pos) {
        this(pos, false);
    }

    public static File coordinatesToFile(Path dir, Coordinate2D coords) {
        String name = "r." + coords.getX() + "." + coords.getZ() + ".mca";
        File f = Paths.get(dir.toString(), name).toFile();

        if (f.exists()) {
            return f;
        } else {
            return null;
        }
    }

    public static McaFile ofCoords(CoordinateDim2D regionCoords) {
        File f = coordinatesToFile(PathUtils.toPath(Config.getWorldOutputDir(), regionCoords.getDimension().getPath(), "region"), regionCoords);

        if (f == null) {
            return null;
        }

        // Load the MCA file - if it cannot be loaded for any reason it's skipped.
        try {
            return new McaFile(f);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets MCA files starting from the center, then increasing in radius.
     */
    public static Stream<McaFile> getFiles(Coordinate2D chunkCenter, Dimension dimension, int radius) {
        Path exportDir = PathUtils.toPath(Config.getWorldOutputDir(), dimension.getPath(), "region");

        if (!exportDir.toFile().exists()) {
            return Stream.empty();
        }
        List<File> files = new ArrayList<>();
        Coordinate2D center = chunkCenter.chunkToRegion().offsetChunk();

        // loop from radius 0 up to the given radius
        for (int r = 0; r < radius; r++) {

            // for each radius, we take only the edge items. That is, for the first and last row we take all files, but
            // for the others we only take the first and last items.
            for (int x = -r; x < r; x++) {
                if (x != r - 1 && x != -r) {
                    File a = McaFile.coordinatesToFile(exportDir, center.add(x, -r));
                    if (a != null) {
                        files.add(a);
                    }

                    File b = McaFile.coordinatesToFile(exportDir, center.add(x, r - 1));
                    if (b != null) {
                        files.add(b);
                    }
                } else {
                    for (int z = -r; z < r; z++) {
                        File f = McaFile.coordinatesToFile(exportDir, center.add(x, z));
                        if (f != null) {
                            files.add(f);
                        }
                    }
                }
            }
        }
        return files.stream().map(el -> {
            try {
                return new McaFile(el);
            } catch (IOException e) {
                return null;
            }
        }).filter(Objects::nonNull);
    }


    /**
     * Read from the save path to see which chunks have been saved already.
     */
    public static Stream<McaFile> getFiles(Dimension dimension, boolean limit) throws IOException {
        Path exportDir = PathUtils.toPath(Config.getWorldOutputDir(), dimension.getPath(), "region");

        if (!exportDir.toFile().exists()) {
            return Stream.empty();
        }

        Stream<File> stream = Files.walk(exportDir)
                .filter(el -> el.getFileName().toString().endsWith(".mca"))
                .map(Path::toFile);

        if (limit) {
            stream = stream.limit(100); // don't load more than 100 region files
        }

        return stream.filter(el -> el.length() > 0)
                .map(el -> {
                    try {
                        return new McaFile(el);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    /**
     * Convert the MCA file into individual chunk data.
     * For details on the MCA file format: https://minecraft.gamepedia.com/Region_file_format
     */
    private HashMap<Integer, ChunkBinary> readFile(File mca) throws IOException {
        FileInputStream inputStream = new FileInputStream(mca);
        byte[] bytes = IOUtils.toByteArray(inputStream);
        inputStream.close();

        // ensure that the data is not empty
        if (bytes.length == 0) {
            return new HashMap<>();
        }

        byte[] locations = Arrays.copyOfRange(bytes, 0, SECTOR_SIZE);
        byte[] timestamps = Arrays.copyOfRange(bytes, SECTOR_SIZE, SECTOR_SIZE * 2);
        byte[] chunkDataArray = Arrays.copyOfRange(bytes, SECTOR_SIZE * 2, bytes.length);

        HashMap<Integer, ChunkBinary> chunkMap = new HashMap<>();

        for (int i = 0; i < locations.length; i += 4) {
            int timestamp = bytesToInt(timestamps, i, i + 3);
            int location = bytesToInt(locations, i, i + 2);
            int size = locations[i + 3] & 0xFF;

            if (size == 0) { continue; }

            // chunk location includes first location/timestamp sections so we need to lower the addresses by 2 sectors
            int chunkDataStart = (location - 2) * SECTOR_SIZE;
            int chunkDataEnd = (location + size - 2) * SECTOR_SIZE;

            // make sure the indices are valid
            if (chunkDataStart < 0 || chunkDataStart >= chunkDataArray.length) {
                continue;
            }
            if (chunkDataEnd < 0 || chunkDataEnd > chunkDataArray.length || chunkDataEnd < chunkDataStart) {
                continue;
            }

            byte[] chunkData = Arrays.copyOfRange(chunkDataArray, chunkDataStart, chunkDataEnd);

            // i is the unique identifier of this chunk within the file, based on coordinates thus consistent
            chunkMap.put(i, new ChunkBinary(timestamp, location, size, chunkData));
        }

        // if a region only has a single small chunk, we ignore it since it's probably not actually generated.
        if (chunkMap.size() == 1) {
            ChunkBinary first = chunkMap.values().iterator().next();
            if (first.getChunkData().length == SECTOR_SIZE) {
                return new HashMap<>();
            }
        }

        return chunkMap;
    }

    /**
     * Converts a number of bytes to a big-endian int.
     * @param arr   the total array of bytes
     * @param start the first byte to use (inclusive)
     * @param end   the last byte to use (INCLUSIVE)
     * @return the integer created from the bytes
     */
    private static int bytesToInt(byte[] arr, int start, int end) {
        int res = 0;
        do {
            res |= (arr[start] & 0xFF) << (end - start) * 8;
        } while (start++ < end);

        return res;
    }

    public void addChunks(Map<Integer, ChunkBinary> chunkMap) {
        // merge new chunks into existing ones
        chunkMap.forEach((key, value) -> this.chunkMap.put(key, value));
    }

    /**
     * Write the MCA file to the given path. Should be called after merge.
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

        // create directory if it doesn't already exist
        Files.createDirectories(filePath.getParent());
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

    private void setLocation(byte[] locations, Integer pos, ChunkBinary chunk) {
        locations[pos] = (byte) (chunk.getLocation() >>> 16);
        locations[pos + 1] = (byte) (chunk.getLocation() >>> 8);
        locations[pos + 2] = (byte) chunk.getLocation();
        locations[pos + 3] = (byte) chunk.getSize();
    }

    private void setTimestamp(byte[] timestamp, int pos, ChunkBinary chunk) {
        timestamp[pos] = (byte) (chunk.getTimestamp() >>> 24);
        timestamp[pos + 1] = (byte) (chunk.getTimestamp() >>> 16);
        timestamp[pos + 2] = (byte) (chunk.getTimestamp() >>> 8);
        timestamp[pos + 3] = (byte) chunk.getTimestamp();
    }

    private void setChunkData(Map<Integer, byte[]> chunkDataList, ChunkBinary chunk) {
        chunkDataList.put(chunk.getLocation(), chunk.getChunkData());
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
     * Return a list of chunk positions of chunks that are present in this file.
     * @return the list of positions.
     */
    public List<CoordinateDim2D> getChunkPositions(Dimension dimension) {
        return chunkMap.keySet().stream()
            .map(num -> intToCoordinate(num).addDimension(dimension))
            .collect(Collectors.toList());
    }

    /**
     * Convert integer position of chunk to global coordinates.
     */
    private Coordinate2D intToCoordinate(int i) {
        int offset = i / 4;
        int localX = offset & 0x1F;
        int localZ = offset >>> 5;

        Coordinate2D actualRegionLocation = regionLocation.offsetRegionToActual();
        return new Coordinate2D(actualRegionLocation.getX() * 32 + localX, actualRegionLocation.getZ() * 32 + localZ);
    }

    /**
     * Convert global coordinates to integer position. We don't have to care about world offsets here because we take
     * the modulo.
     */
    private int coordinateToInt(Coordinate2D c) {
        Coordinate2D regionLocal = c.toRegionLocal();
        return 4 * ((regionLocal.getX() % 32) + (regionLocal.getZ() % 32) * 32);
    }

    public Map<CoordinateDim2D, Chunk> getParsedChunks(Dimension dimension) {
        Map<CoordinateDim2D, Chunk> res = new HashMap<>();
        chunkMap.forEach((key, value) -> res.put(
            new CoordinateDim2D(intToCoordinate(key), dimension),
            value.toChunk(intToCoordinate(key).addDimension(dimension))
        ));
        return res;
    }

    public ChunkBinary getChunkBinary(Coordinate2D coord) {
        return chunkMap.get(coordinateToInt(coord));
    }

    public int countChunks() {
        return chunkMap.size();
    }

    public boolean isEmpty() {
        return chunkMap.isEmpty();
    }

    @Override
    public String toString() {
        return "McaFile{" +
            "pos=" + regionLocation +
            '}';
    }
}
