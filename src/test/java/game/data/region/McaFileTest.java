package game.data.region;

import static org.junit.Assert.assertEquals;

import game.data.Coordinate2D;
import game.data.chunk.ChunkBinary;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class McaFileTest {

    @Test
    public void testRegionCoordinateConversion() {
        HashMap<Integer, ChunkBinary> map = new HashMap<>();
        map.put(coordinateToInt(new Coordinate2D(1, 1)), null);
        map.put(coordinateToInt(new Coordinate2D(2, 5)), null);
        map.put(coordinateToInt(new Coordinate2D(3, 10)), null);
        map.put(coordinateToInt(new Coordinate2D(4, 20)), null);

        Set<Coordinate2D> globalCoords = new HashSet<>(Arrays.asList(
            new Coordinate2D(33, 33),
            new Coordinate2D(34, 37),
            new Coordinate2D(35, 42),
            new Coordinate2D(36, 52)
        ));

        McaFile file = new McaFile(new Coordinate2D(1, 1), map);

        assertEquals(globalCoords, new HashSet<>(file.getChunkPositions()));
    }

    private int coordinateToInt(Coordinate2D coord) {
        return 4 * ((coord.getX() & 31) + (coord.getZ() & 31) * 32);
    }
}
