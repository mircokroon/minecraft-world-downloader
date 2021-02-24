package game.data.entity;

import game.data.entity.metadata.MetaData;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;

public class MobEntity extends Entity {
    private float headPitch;

    private MetaData metaData;

    public MobEntity() {
    }

    @Override
    protected void addNbtData(CompoundTag root) {
        if (metaData != null) {
            metaData.addNbtTags(root);
        }
    }

    public static Entity parse(DataTypeProvider provider) {
        PrimitiveEntity primitive = PrimitiveEntity.parse(provider);
        Entity ent = primitive.getEntity(MobEntity::new);

        if (ent == null) { return null; }

        ent.readPosition(provider);
        ent.yaw = provider.readNext();
        ent.pitch = provider.readNext();
        byte headPitch = provider.readNext();
        parseVelocity(provider);

        if (ent instanceof MobEntity) {
            ((MobEntity) ent).headPitch = headPitch;
        }

        return ent;
    }

    @Override
    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = MetaData.getVersionedMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }
}
