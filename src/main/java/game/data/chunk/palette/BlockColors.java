package game.data.chunk.palette;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import config.Config;

import java.awt.*;
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
        SimpleColor col = colors.get(key);
        if (col != null) {
            return col;
        }

        // handle carpet blocks
        if (key.endsWith("_carpet")) {
            return colors.getOrDefault(key.replace("_carpet", "_wool"), SimpleColor.BLACK);
        }

        // handle stairs & slabs, the original block can have various different name structures
        String suffix = key.endsWith("_slab") ? "_slab" : key.endsWith("_stairs") ? "_stairs" : null;
        if (suffix == null) { return SimpleColor.BLACK; }

        col = colors.get(key.replace(suffix, ""));
        if (col != null) { return col; }

        col = colors.get(key.replace(suffix, "_block"));
        if (col != null) { return col; }

        col = colors.get(key.replace(suffix, "_planks"));
        if (col != null) { return col; }

        col = colors.get(key.replace(suffix, "s"));
        if (col != null) { return col; }

        return SimpleColor.BLACK;
    }

    /**
     * We only have colours for 'solid' blocks.
     * @param key the block ID
     */
    public boolean isSolid(String key) {
        if (key.endsWith("air")) {
            return false;
        }
        return getColor(key) != SimpleColor.BLACK;
    }
}