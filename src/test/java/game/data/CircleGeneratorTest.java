package game.data;

import static org.assertj.core.api.Assertions.assertThat;

import game.data.coordinates.Coordinate2D;
import java.util.List;
import org.junit.jupiter.api.Test;

class CircleGeneratorTest {
    @Test
    public void circleTest() {
        CircleGenerator g = new CircleGenerator();
        g.computeUpToRadius(16);
        List<List<Coordinate2D>> circles = g.getResult();


        Coordinate2D center = new Coordinate2D(0, 0);
        for (int radius = 0; radius < circles.size(); radius++) {
            for (Coordinate2D coord : circles.get(radius)) {
                assertThat(center.isInRangeEuclidean(coord, radius))
                    .withFailMessage(coord + " not in range " + radius)
                    .isTrue();
                assertThat(center.isInRangeEuclidean(coord, radius - 1))
                    .withFailMessage(coord + " should not be in range " + (radius - 1))
                    .isFalse();
            }
        }
    }

}