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

    @Override
    protected void parseRotation(DataTypeProvider provider) {
        this.yaw = provider.readNext();
        this.pitch = provider.readNext();
        this.headPitch = provider.readNext();
    }

    @Override
    protected void parseFully(DataTypeProvider provider) {
        super.parseFully(provider);

        parseVelocity(provider);
    }

    public static Entity parse(DataTypeProvider provider) {
        PrimitiveEntity primitive = PrimitiveEntity.parse(provider);
        Entity ent = primitive.getLivingEntity();

        ent.parseFully(provider);

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
