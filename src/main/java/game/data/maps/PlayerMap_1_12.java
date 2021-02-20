package game.data.maps;

import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.Collections;
import java.util.stream.Collectors;

public class PlayerMap_1_12 extends PlayerMap {
    public PlayerMap_1_12(int id) {
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

        int iconCount = provider.readVarInt();
        for (int i = 0; i < iconCount; i++) {
            provider.readNext();
            provider.readNext();
            provider.readNext();
        }

        parseMapImage(provider);
    }

    @Override
    public void addNbtTags(CompoundTag data) {
        data.add("width", new ShortTag((short) 128));
        data.add("height", new ShortTag((short) 128));

        data.add("xCenter", new IntTag(Integer.MAX_VALUE));
        data.add("zCenter", new IntTag(Integer.MAX_VALUE));
    }
}
