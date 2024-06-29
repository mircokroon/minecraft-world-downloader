package game.data.dimension;

import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * DimensionType specifies the dimension information that is sent by the server when the player spawns. It is used to
 * specify dimension parameters that are different in the nether and end, such as being able to sleep or how fast
 * lava flows.
 */
public class DimensionType {
    private static final CompoundTag PROPERTIES_OVERWORLD_DEFAULT;
    private static final CompoundTag PROPERTIES_DEFAULT;

    static {
        PROPERTIES_OVERWORLD_DEFAULT = new CompoundTag();
        PROPERTIES_OVERWORLD_DEFAULT.add("min_y", new IntTag(-64));
        PROPERTIES_OVERWORLD_DEFAULT.add("height", new IntTag(384));

        PROPERTIES_DEFAULT = new CompoundTag();
        PROPERTIES_DEFAULT.add("min_y", new IntTag(0));
        PROPERTIES_DEFAULT.add("height", new IntTag(256));
    }

    private final String namespace;
    private final String name;
    private final int index;
    private final int signature;
    private CompoundTag properties;

    // default values
    private int dimensionMinHeight = 0;
    private int dimensionMaxHeight = 256;

    // after 1.20.6
    DimensionType(String namespace, String name, int id, CompoundTag properties) {
        this.properties = properties;
        this.index = id;
        this.namespace = namespace;
        this.name = name;
        this.signature = this.properties == null ? 0 : this.properties.hashCode();

        if (properties == null) {
            setPropertiesFromName();
        }

        parseHeights();
    }

    // before 1.20.6
    DimensionType(String namespace, String name, CompoundTag tag) {
        this.properties = (CompoundTag) tag.get("element");
        this.index = ((IntTag) tag.get("id")).value;
        this.namespace = namespace;
        this.name = name;
        this.signature = this.properties.hashCode();

        parseHeights();
    }

    private void setPropertiesFromName() {
        if (!this.namespace.equals("minecraft")) { return; }

        if (this.name.equals("overworld")) {
            this.properties = PROPERTIES_OVERWORLD_DEFAULT;
        } else {
            this.properties = PROPERTIES_DEFAULT;
        }
    }

    private void parseHeights() {
        if (this.properties == null) {
            return;
        }

        var minTag = this.properties.get("min_y");
        var heightTag = this.properties.get("height");

        if (minTag.isError() || heightTag.isError()) {
            return;
        }

        this.dimensionMinHeight = minTag.intValue();
        this.dimensionMaxHeight = heightTag.intValue();
    }

    public CompoundTag getProperties() {
        return properties;
    }

    /**
     * We use the hash of the dimension properties as a signature that we can use to find out which dimension we
     * actually joined. This is not perfect and should be changed if the server will ever let us know which dimension
     * type each dimension has.
     */
    public int getSignature() {
        return signature;
    }

    /**
     * Write the dimension type data to the dimension_type directory.
     */
    public void write(Path prefix) throws IOException {
        Path destination = PathUtils.toPath(prefix.toString(), namespace, "dimension_type", name + ".json");
        Files.createDirectories(destination.getParent());

        Files.write(destination, Collections.singleton(DimensionRegistry.GSON.toJson(properties)));
    }

    /**
     * Returns fully qualified name.
     */
    public String getName() {
        return namespace + ":" + name;
    }

    public int getDimensionMinHeight() {
        return dimensionMinHeight;
    }

    public int getDimensionMaxHeight() {
        return dimensionMaxHeight;
    }
}
