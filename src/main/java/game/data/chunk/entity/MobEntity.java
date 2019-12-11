package game.data.chunk.entity;

import game.data.chunk.entity.metadata.MetaData;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.DoubleTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.ListTag;

import java.util.List;

public class MobEntity extends Entity {
    private float yaw;
    private float pitch;
    private float headPitch;

    private MetaData metaData;

    private MobEntity() { }

    @Override
    protected void addNbtData(CompoundTag root) {
        List<FloatTag> pos = List.of(new FloatTag(pitch), new FloatTag(yaw));
        root.add("Rotation", new ListTag(ListTag.TAG_FLOAT, pos));

        metaData.addNbtTags(root);
    }

    @Override
    protected void parseRotation(DataTypeProvider provider) {
        this.yaw = provider.readNext();
        this.pitch = provider.readNext();
        this.headPitch = provider.readNext();
    }

    public static Entity parse(DataTypeProvider provider) {
        return Entity.parseEntity(provider, new MobEntity());
    }

    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = MetaData.getVersionedMetaData();
        }
        metaData.parse(provider);
    }
}
