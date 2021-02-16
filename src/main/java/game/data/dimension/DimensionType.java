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

    private final String namespace;
    private final String name;
    private final int index;
    private final int signature;

    private CompoundTag properties;

    DimensionType(String namespace, String name, CompoundTag tag) {
        this.properties = (CompoundTag) tag.get("element");
        this.index = ((IntTag) tag.get("id")).value;
        this.namespace = namespace;
        this.name = name;
        this.signature = this.properties.hashCode();

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

        Files.write(destination, Collections.singleton(DimensionCodec.GSON.toJson(properties)));
    }

    /**
     * Returns fully qualified name.
     */
    public String getName() {
        return namespace + ":" + name;
    }
}
