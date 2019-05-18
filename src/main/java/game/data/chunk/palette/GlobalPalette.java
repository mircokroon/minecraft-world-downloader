package game.data.chunk.palette;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class GlobalPalette {
    HashMap<Integer, BlockState> states;

    public GlobalPalette(String version) {
        this.states = new HashMap<>();

        String file = "blocks-" + version + ".json";
        InputStream input = GlobalPalette.class.getClassLoader().getResourceAsStream(file);

        // if the file doesn't exist, there is no palette for this version.
        if (input == null) { return; }

        JsonResult map = new Gson().fromJson(new InputStreamReader(input), JsonResult.class);
        map.forEach((name, type) -> type.states.forEach(state -> {
            states.put(state.id, new BlockState(name, state.properties));
        }));
    }

    public BlockState getState(int key) {
        return states.getOrDefault(key, null);
    }
}

class JsonResult extends HashMap<String, JsonBlockType> { }
