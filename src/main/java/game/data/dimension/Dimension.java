package game.data.dimension;

import static util.ExceptionHandling.attempt;

import com.google.gson.Gson;
import config.Config;
import config.Version;
import game.data.WorldManager;
import game.data.chunk.version.Chunk_1_17;
import se.llbit.nbt.SpecificTag;
import util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class to hold both custom and default dimensions. For custom dimensions, it can write a partial definition file.
 * Server does not tell us how the world is generated, but we can use an empty superflat generator which ensures
 * no new chunks are generated.
 */
public class Dimension {
    public static final Dimension OVERWORLD = new Dimension("minecraft", "overworld", "minecraft:overworld");
    public static final Dimension NETHER = new Dimension("minecraft", "the_nether", "minecraft:the_nether");
    public static final Dimension END = new Dimension("minecraft", "the_end", "minecraft:the_end");
    public static final List<Dimension> DEFAULTS = Arrays.asList(OVERWORLD, NETHER, END);

    private final String namespace;
    private final String name;
    private String type;

    Dimension(String namespace, String name, String type) {
        this(namespace, name);
        this.type = type;
    }

    Dimension(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getType() {
        if (type == null) { return "minecraft:overworld"; }
        return type;
    }

    /**
     * In pre-1.16 versions the dimension used to be identified by a numeric ID.
     */
    public static Dimension fromId(int id) {
        switch (id) {
            case -1: return NETHER;
            case 1: return END;
            default: return OVERWORLD;
        }
    }

    /**
     * Find the dimension from it's identifier name. For custom dimensions we need to consult the codec.
     */
    public static Dimension fromString(String readString) {
        switch (readString) {
            case "minecraft:the_end": return END;
            case "minecraft:the_nether": return NETHER;
            case "minecraft:overworld": return OVERWORLD;
            default: {
                DimensionCodec codec = WorldManager.getInstance().getDimensionCodec();
                if (codec == null) { return OVERWORLD; }

                Dimension dim = codec.getDimension(readString);
                if (dim == null) { return OVERWORLD; }

                return dim;
            }
        }
    }

    public static Dimension standardDimensionFromString(String string) {
        switch (string) {
            case "minecraft:the_end": return END;
            case "minecraft:the_nether": return NETHER;
            default: return OVERWORLD;
        }
    }

    /**
     * Path where the world should be saved to. For custom dimensions it depends on the name and namespace.
     */
    public String getPath() {
        if (namespace.equals("minecraft")) {
            switch (name) {
                case "the_nether": return "DIM-1";
                case "the_end": return "DIM1";
                case "overworld": return"";
            }
            return name;
        }

        return Paths.get("dimensions", namespace, name).toString();
    }

    /**
     * Write the dimension data to the dimension directory.
     */
    public void write(Path prefix) throws IOException {
        Path destination = PathUtils.toPath(prefix.toString(), namespace, "dimension", name + ".json");
        Files.createDirectories(destination.getParent());

        DimensionDefinition definition = new DimensionDefinition(type);

        Files.write(destination, Collections.singleton(new Gson().toJson(definition)));
    }

    /**
     * When we join a dimension, we can use the dimension type information to try and link this to the registered
     * type in the codec. For 1.17+ we need to give the world height/depth information to the chunk.
     */
    public void registerType(SpecificTag dimensionNbt) {
        if (Config.versionReporter().isAtLeast(Version.V1_17)) {
            Chunk_1_17.setWorldHeight(dimensionNbt.get("min_y").intValue(), dimensionNbt.get("height").intValue());
        }

        if (this.type != null) {
            return;
        }

        int hash = dimensionNbt.hashCode();
        DimensionType type = WorldManager.getInstance().getDimensionCodec().getDimensionType(hash);
        if (type != null) {
            this.type = type.getName();

            // re-write since we write the dimension information on join otherwise
            attempt(() -> write(PathUtils.toPath(Config.getWorldOutputDir(), "datapacks", "downloaded", "data")));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dimension dimension = (Dimension) o;

        if (!Objects.equals(namespace, dimension.namespace)) return false;
        return Objects.equals(name, dimension.name);
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return namespace + ":" + name;
    }

    public String getName() {
        return this.toString();
    }

    /**
     * 1.19+ gets the dimension type as a string (this actually makes sense unlike the previous
     * implementation).
     */
    public void setType(String dimensionType) {
        DimensionType type =
            WorldManager.getInstance().getDimensionCodec().getDimensionType(dimensionType);

        if (type == null) {
            return;
        }
        this.type = type.getName();

        int minY = type.getProperties().get("min_y").intValue();
        int height = type.getProperties().get("height").intValue();
        Chunk_1_17.setWorldHeight(minY, height);

        // re-write since we write the dimension information on join otherwise
        attempt(() -> write(PathUtils.toPath(Config.getWorldOutputDir(), "datapacks", "downloaded", "data")));
    }

    public boolean isNether() {
        return this == NETHER;
    }
}

/**
 * Class to hold a dimension definition file. We need to be able to modify the type so storing it in a resource file is
 * more hassle than this.
 */
class DimensionDefinition {
    private String type = "minecraft:overworld";
    private final Generator generator = new Generator();

    public DimensionDefinition(String type) {
        if (type != null) {
            this.type = type;
        }
    }

    static class Generator {
        private final String type = "minecraft:flat";
        private final int seed = 0;
        private final Settings settings = new Settings();

        static class Settings {
            private final byte[] layers = new byte[0];
            private final HashMap<String, HashMap> structures;

            public Settings() {
                structures = new HashMap<>();
                structures.put("structures", new HashMap<>());
            }
        }
    }
}
