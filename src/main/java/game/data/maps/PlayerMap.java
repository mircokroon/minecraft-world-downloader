package game.data.maps;

import config.Config;
import config.Option;
import config.Version;
import game.data.WorldManager;
import game.data.dimension.Dimension;
import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PlayerMap {
    int id;
    byte scale;
    boolean trackingPosition;
    boolean locked;
    byte[] colors;

    Dimension dimension;
    int xCenter;
    int zCenter;

    List<Icon> icons;

    public PlayerMap(int id) {
        this.icons = new ArrayList<>();
        this.id = id;
        this.dimension = WorldManager.getInstance().getDimension();
        this.xCenter = 0;
        this.zCenter = 0;
        this.colors = new byte[128 * 128];
    }

    public static PlayerMap getVersioned(int id) {
        return Config.versionReporter().select(PlayerMap.class,
                Option.of(Version.V1_14, () -> new PlayerMap_1_14(id)),
                Option.of(Version.V1_12, () -> new PlayerMap_1_12(id))
        );
    }

    public SpecificTag toNbt() {
        CompoundTag data = new CompoundTag();

        data.add("xCenter", new IntTag(xCenter));
        data.add("zCenter", new IntTag(zCenter));
        data.add("colors", new ByteArrayTag(colors));

        // disable tracking since we don't know the center
        data.add("trackingPosition", new ByteTag(0));
        data.add("unlimitedTracking", new ByteTag(0));


        data.add("scale", new ByteTag(1));

        CompoundTag root = new CompoundTag();
        root.add("data", data);
        root.add("DataVersion", new IntTag(Config.getDataVersion()));

        addNbtTags(data);

        return root;
    }

    protected void addNbtTags(CompoundTag data) { };


    protected void parseMapImage(DataTypeProvider provider) {
        int columns = provider.readNext() & 0xFF;
        if (columns == 0) { return; }

        int rows = provider.readNext() & 0xFF;
        int firstCol = provider.readNext() & 0xFF;
        int firstRow = provider.readNext() & 0xFF;
        int length = provider.readVarInt();
        byte[] colUpdate = provider.readByteArray(length);

        // in most cases we'll just get a full map, so we don't have to loop over the data
        if (rows == 128 && columns == 128) {
            this.colors = colUpdate;
        } else {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    int curRow = firstRow + row;
                    int curCol = firstCol + col;
                    this.colors[curCol + curRow * 128] = colUpdate[col + row * columns];
                }
            }
        }
    }

    public abstract void parse(DataTypeProvider provider);
}

class Icon {
    static Map<Integer, String> colors;
    static {
        colors = new HashMap<>();
        colors.put(10, "white");
        colors.put(11, "orange");
        colors.put(12, "magenta");
        colors.put(13, "light blue");
        colors.put(14, "yellow");
        colors.put(15, "lime");
        colors.put(16, "pink");
        colors.put(17, "gray");
        colors.put(18, "light gray");
        colors.put(19, "cyan");
        colors.put(20, "purple");
        colors.put(21, "blue");
        colors.put(22, "brown");
        colors.put(23, "green");
        colors.put(24, "red");
        colors.put(25, "black");
    }
    int x, z;
    String name = "";
    String color;

    SpecificTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.add("Name", new StringTag(name));
        tag.add("Color", new StringTag(color));

        CompoundTag pos = new CompoundTag();
        pos.add("X", new IntTag(x));
        pos.add("Y", new IntTag(0));
        pos.add("Z", new IntTag(z));

        tag.add("Pos", pos);

        return tag;
    }

    static Icon of(DataTypeProvider provider) {
        Icon icon = new Icon();

        int type = provider.readVarInt();
        icon.x = provider.readNext();
        icon.z = provider.readNext();

        int direction = provider.readNext();

        boolean hasName = provider.readBoolean();
        if (hasName) {
            icon.name = provider.readChat();
        }

        // we need to read the full icon so we stay synchronized with the packet, even if we will discard it
        icon.color = colors.get(type);
        if (icon.color == null) {
            return null;
        }
        return icon;
    }
}
