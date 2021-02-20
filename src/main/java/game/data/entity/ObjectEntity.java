package game.data.entity;

import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.FloatTag;
import se.llbit.nbt.ListTag;

import java.util.Arrays;
import java.util.List;

public class ObjectEntity extends Entity {
    protected ObjectEntity() { }

    @Override
    protected void addNbtData(CompoundTag root) {

    }

    @Override
    protected void parseRotation(DataTypeProvider provider) {
        this.pitch = provider.readNext();
        this.yaw = provider.readNext();
    }

    @Override
    protected void parseFully(DataTypeProvider provider) {
        super.parseFully(provider);

        parseData(provider);
        parseVelocity(provider);
    }

    protected void parseData(DataTypeProvider provider) {
        provider.readInt();
    }

    public static Entity parse(DataTypeProvider provider) {
        PrimitiveEntity primitive = PrimitiveEntity.parse(provider);
        Entity ent = primitive.getObjectEntity();

        ent.parseFully(provider);

        return ent;
    }
}
