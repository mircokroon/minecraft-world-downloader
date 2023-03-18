package game.data.chunk.palette;

import config.Config;
import game.data.registries.RegistryLoader;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import se.llbit.nbt.CompoundTag;

/**
 * This class manages the global palettes. It can hold not only a palette for the current game version, but also for
 * different versions when these are needed to load previously saved chunks.
 */
public final class GlobalPaletteProvider {
    private GlobalPaletteProvider() { }

    private static HashMap<Integer, GlobalPalette> palettes = new HashMap<>();
    private static Queue<BlockState> uninitialised;

    /**
     * Retrieves a global palette based on the data version number. If the palette is not already known, it will be
     * created through requestPalette.
     */
    public static GlobalPalette getGlobalPalette(int dataVersion) {
        GlobalPalette palette = palettes.get(dataVersion);

        if (palette == null) {
            return requestPalette(dataVersion);
        }
        return palette;
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

            if (uninitialised != null) {
                while (!uninitialised.isEmpty()) {
                    p.addBlockState(uninitialised.remove());
                }
            }
            return p;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void registerBlock(String name, int id) {
        GlobalPalette palette = palettes.get(Config.getDataVersion());

        BlockState state = new BlockState(name, id, new CompoundTag());

        if (palette == null) {
            enqueueBlock(state);
            return;
        }

        palette.addBlockState(state);
    }

    private static void enqueueBlock(BlockState state) {
        if (uninitialised == null) {
            uninitialised = new ConcurrentLinkedDeque<>();
        }
        uninitialised.add(state);
    }
}
