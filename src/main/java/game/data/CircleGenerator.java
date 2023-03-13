package game.data;

import game.data.coordinates.Coordinate2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate lists of coordinates for chunks per radius distance.
 */
public class CircleGenerator {
    private final List<List<Coordinate2D>> circles = new ArrayList<>();

    public void computeUpToRadius(int radius) {
        for (int i = 0; i < radius + 1; i++) {
            circles.add(new ArrayList<>());
        }

        int grid = radius * 2 + 1;
        for (int x = 0; x < grid; x++) {
            for (int z = 0; z < grid; z++) {
                int centerX = x - radius;
                int centerZ = z - radius;

                int dist = distance(centerX, centerZ);

                if (centerX == 0 || centerZ == 0) {
                    dist += 1;
                }

                // skip small radius
                if (dist > radius || dist < 3) {
                    continue;
                }

                circles.get(dist).add(new Coordinate2D(centerX, centerZ));
            }
        }
    }

    private int distance(int x, int z) {
        double dist = Math.sqrt(x * x + z * z);

        return (int) Math.ceil(dist);
    }

    public List<List<Coordinate2D>> getResult() {
        return circles;
    }
}
