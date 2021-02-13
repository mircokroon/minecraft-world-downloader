package game.data.chunk.palette;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Holds a map of block colors for colouring the overview map.
 */
public class BlockColors {
    private HashMap<String, SimpleColor> colors;

    private BlockColors() { }

    /**
     * Instantiate a block colors object from the default file.
     */
    public static BlockColors create() {
        String file = "block-colors.json";
        InputStream input = BlockColors.class.getClassLoader().getResourceAsStream(file);

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(SimpleColor.class, (JsonDeserializer<SimpleColor>)
                (el, type, ctx) -> new SimpleColor(el.getAsInt()));

        return builder.create().fromJson(new InputStreamReader(input), BlockColors.class);
    }

    /**
     * Get a block color from a given block name.
     */
    public SimpleColor getColor(String key) {
        return colors.getOrDefault(key, SimpleColor.BLACK);
    }

    /**
     * We only have colours for 'solid' blocks.
     * @param key the block ID
     */
    public boolean isSolid(String key) {
        return colors.containsKey(key);
    }
}