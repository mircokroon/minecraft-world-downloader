package game.data.chunk;

import game.data.chunk.palette.BlockState;
import game.data.chunk.palette.SimpleColor;
import game.data.coordinates.Coordinate3D;
import game.data.dimension.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class ChunkHeightHandler {
    private int[] heightMap;
    private int[] heightMapBelowBedrock;
    private List[] caves;
    Chunk c;

    public ChunkHeightHandler(Chunk c) {
        this.c = c;

        this.computeHeightMap();
    }

    private void computeHeightMap() {
        if (this.heightMap != null) {
            return;
        }

        this.heightMapBelowBedrock = new int[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH];
        this.heightMap = new int[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH];
        this.caves = new List[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH];
        for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
            for (int z = 0; z < Chunk.SECTION_WIDTH; z++) {
                heightMapBelowBedrock[z << 4 | x] = computeHeight(x, z, true);
                heightMap[z << 4 | x] = computeHeight(x, z, false);
                caves[z << 4 | x] = findCaves(x, z);
            }
        }
    }

    /**
     * Computes the height at a given location. When we are in the nether, we want to try and make it clear where there
     * is an opening, and where there is not. For this we skip the first two chunks sections (these will be mostly solid
     * anyway, but may contain misleading caves). We then only count blocks after we've found some air space.
     */
    private int computeHeight(int x, int z, boolean ignoredBedrockAbove) {
        // if we're in the Nether, we want to find an air block before we start counting blocks.
        boolean isNether = ignoredBedrockAbove && c.location.getDimension().equals(Dimension.NETHER);
        int topSection = isNether ? 5 : c.getMaxBlockSection();

        MutableBoolean foundAir = new MutableBoolean(!isNether);

        for (int sectionY = topSection; sectionY >= c.getMinBlockSection(); sectionY--) {
            ChunkSection cs = c.getChunkSection(sectionY);
            if (cs == null) {
                foundAir.setTrue();
                continue;
            }

            int height = cs.computeHeight(x, z, foundAir);

            if (height < 0) { continue; }

            // if we're in the nether we can't find
            if (isNether && sectionY == topSection && height == 15) {
                return 127;
            }
            return (sectionY * Chunk.SECTION_HEIGHT) + height;
        }
        return isNether ? 127 : 0;
    }

    /**
     * We need to update the image only if the updated block was either the top layer, or above the top layer.
     * Technically this does not take transparent blocks into account, but that's fine.
     */
    public boolean updateHeight(Coordinate3D coords) {
        if (coords.getY() >= heightAt(coords.getX(), coords.getZ())) {
            recomputeHeight(coords.getX(), coords.getZ());

            return true;
        }
        return false;
    }

    /**
     * Recompute the heights in the given coordinate collection. We keep track of which heights actually changed, and
     * only redraw if we have to.
     */
    public boolean recomputeHeights(Collection<Coordinate3D> toUpdate) {
        boolean hasChanged = false;
        for (Coordinate3D pos : toUpdate) {
            if (pos.getY() >= heightAt(pos.getX(), pos.getZ())) {
                hasChanged |= recomputeHeight(pos.getX(), pos.getZ());
            }
        }
        return hasChanged;
    }

    private boolean recomputeHeight(int x, int z) {
        int beforeAboveBedrock = heightMapBelowBedrock[z << 4 | x];
        int before = heightMap[z << 4 | x];

        int afterAboveBedrock = computeHeight(x, z, true);
        heightMapBelowBedrock[z << 4 | x] = afterAboveBedrock;

        int after = computeHeight(x, z, false);
        heightMap[z << 4 | x] = after;
        caves[z << 4 | x] = getCaves(x, z);

        return before != after || beforeAboveBedrock != afterAboveBedrock;
    }

    public int heightAt(int x, int z) {
        return heightAt(x, z, false);
    }

    public int heightAt(int x, int z, boolean belowBedrock) {
        return belowBedrock ? heightMapBelowBedrock[z << 4 | x] : heightMap[z << 4 | x];
    }

    private List<Cave> findCaves(int x, int z) {
        int surface = heightAt(x, z);
        surface = Math.min(60, surface);

        List<Cave> caves = new ArrayList<>();

        int base = c.getMinBlockSection() * Chunk.SECTION_HEIGHT;
        BlockState state = null;

        Cave cave = null;
        boolean inCave = false;
        for (int y = base; y < surface; y++) {
            BlockState curState = c.getBlockStateAt(x, y, z);

            boolean isEmpty = curState == null || curState.getColor() == SimpleColor.BLACK;
            if (inCave && isEmpty) {
                cave.addDepth();
            } else if (inCave) {
                inCave = false;
            } else if (isEmpty && state != null) {
                cave = new Cave(y, state);
                caves.add(cave);
                inCave = true;
            }
            state = curState;
        }

        return caves;
    }

    public List<Cave> getCaves(int x, int z) {
        return caves[z << 4 | x];
    }
}
