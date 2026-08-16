package game.data.entity;

import config.Config;
import config.Version;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;

import static util.PrintUtils.devPrint;

public class ObjectEntity extends Entity {
    protected ObjectEntity() { }

    @Override
    protected void addNbtData(CompoundTag root) { }

    protected void setData(int data) { }

    public static Entity parse(DataTypeProvider provider) {
        PrimitiveEntity primitive = PrimitiveEntity.parse(provider);
        Entity ent = primitive.getEntity(ObjectEntity::new);

        if (ent == null) { return null; }

        devPrint("[spawn_entity] id=" + primitive.id + " type=" + primitive.typeName
            + " remaining after id/uuid/type=" + provider.remaining());

        ent.readPosition(provider);

        devPrint("[spawn_entity] id=" + primitive.id + " remaining after position=" + provider.remaining());

        // as of 26.1, velocity moved from the end of the packet to right after the position
        boolean velocityBeforeRotation = Config.versionReporter().isAtLeast(Version.V26_1);
        if (velocityBeforeRotation) {
            parseVelocity(provider);
        }

        ent.pitch = provider.readNext();
        ent.yaw = provider.readNext();
        int data;
        if (Config.versionReporter().isAtLeast(Version.V1_19)) {
            provider.readNext(); // head rotation
            data = provider.readVarInt();
        } else {
            data = provider.readInt();
        }

        if (!velocityBeforeRotation) {
            parseVelocity(provider);
        }

        // only if it's an ObjectEntity do we actually set the data bit
        if (ent instanceof ObjectEntity) {
            ((ObjectEntity) ent).setData(data);
        }

        return ent;
    }
}
