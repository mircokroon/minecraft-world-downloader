package game.data.chunk.palette;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Holds a map of block colors for colouring the overview map.
 */
public class BlockColors {
    private HashMap<String, Integer> colors;

    private BlockColors() { }

    /**
     * Instantiate a block colors object from the default file.
     */
    public static BlockColors create() {
        String file = "block-colors.json";
        InputStream input = BlockColors.class.getClassLoader().getResourceAsStream(file);

        return new Gson().fromJson(new InputStreamReader(input), BlockColors.class);
    }

    /**
     * Get a block color from a given block name.
     */
    public int getColor(String key) {
        return colors.getOrDefault(key, 0);
    }

    /**
     * We only have colours for 'solid' blocks.
     * @param key the block ID
     */
    public boolean isSolid(String key) {
        return colors.containsKey(key);
    }
}