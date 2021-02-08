package game.data.chunk.palette;

import game.data.chunk.Chunk;

import java.util.*;

public class PaletteBuilder {
    private final int[] blockIndices;
    private final LinkedHashSet<Integer> blocks;

    public PaletteBuilder(int[][][] blockStates) {
        this.blockIndices = new int[Chunk.SECTION_WIDTH * Chunk.SECTION_WIDTH * Chunk.SECTION_HEIGHT];
        this.blocks = new LinkedHashSet<>();

        Map<Integer, Integer> indices = new HashMap<>();
        for (int y = 0; y < Chunk.SECTION_WIDTH; y++) {
            for (int z = 0; z < Chunk.SECTION_HEIGHT; z++) {
                for (int x = 0; x < Chunk.SECTION_WIDTH; x++) {
                    int state = blockStates[x][y][z];
                    int blockNumber = (((y * Chunk.SECTION_HEIGHT) + z) * Chunk.SECTION_WIDTH) + x;


                    if (blocks.contains(state)) {
                        this.blockIndices[blockNumber] = indices.get(state);
                    } else {
                        int index = blocks.size();
                        indices.put(state, index);
                        blocks.add(state);

                        this.blockIndices[blockNumber] = index;
                    }
                }
            }
        }
    }

    public Palette build() {
        int[] arr = new int[blocks.size()];

        int i = 0;
        for (int v : blocks) {
            arr[i++] = v;
        }

        return new Palette(arr);
    }

    public int[] getIndices() {
        return blockIndices;
    }


}
