package game.data.chunk.palette;

import config.Config;
import game.protocol.ProtocolVersionHandler;
import game.data.registries.RegistryLoader;
import game.protocol.Protocol;

import java.io.IOException;
import java.util.HashMap;

/**
 * This class manages the global palettes. It can hold not only a palette for the current game version, but also for
 * different versions when these are needed to load previously saved chunks.
 */
public final class GlobalPaletteProvider {
    private GlobalPaletteProvider() { }

    private static HashMap<Integer, GlobalPalette> palettes = new HashMap<>();

    /**
     * Retrieves a global palette based on the data version number. If the palette is not already known, it will be
     * created through requestPalette.
     */
    public static GlobalPalette getGlobalPalette(int dataVersion) {
        if (palettes.containsKey(dataVersion)) {
            return palettes.get(dataVersion);
        }
        return requestPalette(dataVersion);
    }

    /**
     * If no data version is specified, the current game version is used instead.
     */
    public static GlobalPalette getGlobalPalette() {
        return getGlobalPalette(Config.getDataVersion());
    }

    /**
     * To request a palette we make use of the registry loader, which needs a textual game version. The protocol
     * version handler has this value for us. The registry loader will load it either from a previously generated
     * report, or it will download the relevant Minecraft version and generate it.
     */
    private static GlobalPalette requestPalette(int dataVersion) {
        Protocol version = ProtocolVersionHandler.getInstance().getProtocolByDataVersion(dataVersion);
        try {
            GlobalPalette p = RegistryLoader.forVersion(version.getVersion()).generateGlobalPalette();
            palettes.put(dataVersion, p);
            return p;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
