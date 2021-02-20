package game.data.maps;

import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.Collections;
import java.util.stream.Collectors;

public class PlayerMap_1_14 extends PlayerMap {
    public PlayerMap_1_14(int id) {
        super(id);
    }

    @Override
    public SpecificTag toNbt() {
        return super.toNbt();
    }

    @Override
    public void parse(DataTypeProvider provider) {
        byte scale = provider.readNext();
        if (scale != 0) {
            this.scale = scale;
        }

        this.trackingPosition = provider.readBoolean();
        this.locked = provider.readBoolean();

        parseIcons(provider);
        parseMapImage(provider);
    }

    public void parseIcons(DataTypeProvider provider) {
        int iconCount = provider.readVarInt();
        for (int i = 0; i < iconCount; i++) {
            Icon icon = Icon.of(provider);
            if (icon != null) {
                icons.add(icon);
            }
        }
    }

    @Override
    public void addNbtTags(CompoundTag data) {
        data.add("dimension", new StringTag(dimension.toString()));
        data.add("banners", new ListTag(Tag.TAG_COMPOUND, icons.stream().map(Icon::toNbt).collect(Collectors.toList())));
        data.add("frames", new ListTag(Tag.TAG_COMPOUND, Collections.emptyList()));

        // we lock the map and set the scale to 1, these don't matter for the client anyway
        data.add("locked", new ByteTag(1));
    }
}
