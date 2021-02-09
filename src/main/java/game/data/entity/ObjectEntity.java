package game.data.entity;

import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.ListTag;

import java.util.Arrays;
import java.util.List;

public class ObjectEntity extends Entity {
    private float pitch;
    private float yaw;

    private ObjectEntity() { }


    @Override
    protected void addNbtData(CompoundTag root) {
        List<FloatTag> pos = Arrays.asList(new FloatTag(pitch), new FloatTag(yaw));
        root.add("Rotation", new ListTag(ListTag.TAG_FLOAT, pos));
    }

    @Override
    protected void parseRotation(DataTypeProvider provider) {
        this.pitch = provider.readNext();
        this.yaw = provider.readNext();
    }

    public static Entity parse(DataTypeProvider provider) {
        return Entity.parseEntity(provider, new ObjectEntity());
    }
}
