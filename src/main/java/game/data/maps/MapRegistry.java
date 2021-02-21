package game.data.maps;

import config.Config;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.Tag;
import util.NbtUtil;
import util.PathUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static util.ExceptionHandling.attempt;

/**
 * Handle storing of the map item.
 */
public class MapRegistry {
    private final String registryFileName = "idcounts.dat";
    private final Path dataDir = PathUtils.toPath(Config.getWorldOutputDir(),  "data");
    private final File registryFile = Paths.get(dataDir.toString(), registryFileName).toFile();

    private final Map<Integer, PlayerMap> maps;
    private final Set<Integer> updatedSince;
    private int maxMapId = 0;

    private final ExecutorService executor;

    public MapRegistry() {
        this.maps = new ConcurrentHashMap<>();
        this.updatedSince = ConcurrentHashMap.newKeySet();

        try {
            read();
        } catch (IOException e) {
            e.printStackTrace();
            // if we can't read it, thats fine, we'll use maxId = 0
        }

        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Map Parser Service"));;
    }

    /**
     * Read in the maximum map ID from the existing data file, if it exists.
     */
    private void read() throws IOException {
        if (!registryFile.exists()) {
            return;
        }

        Tag root = NbtUtil.read(registryFile).unpack().get("data");

        this.maxMapId = root.get("map").intValue();
    }

    /**
     * Save the number of maps in the idcount file, then save each map that changed since we saved last time.
     */
    public void save() throws IOException {
        Files.createDirectories(dataDir);

        CompoundTag data = new CompoundTag();
        data.add("map", new IntTag(maxMapId));
        CompoundTag root = new CompoundTag();
        root.add("data", data);

        NbtUtil.write(new NamedTag("", root), registryFile.toPath());

        updatedSince.forEach((id) -> {
            PlayerMap map = maps.get(id);
            try {
                NbtUtil.write(new NamedTag("", map.toNbt()), Paths.get(dataDir.toString(), "map_" + id + ".dat"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        updatedSince.clear();
    }

    /**
     * Read map from network packet to class.
     */
    public void readMap(DataTypeProvider provider) {
        executor.execute(() -> attempt(() -> {
            int mapId = provider.readVarInt();
            this.updatedSince.add(mapId);
            if (mapId > maxMapId) {
                maxMapId = mapId;
            }

            PlayerMap map = maps.computeIfAbsent(mapId, PlayerMap::getVersioned);
            map.parse(provider);
        }));
    }

    public int countActiveMaps() {
        return this.maps.size();
    }
}
