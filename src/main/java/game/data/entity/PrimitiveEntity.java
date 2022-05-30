package game.data.entity;

import java.util.function.Supplier;

import game.data.WorldManager;
import game.data.entity.specific.ArmorStand;
import game.data.entity.specific.Cat;
import game.data.entity.specific.ItemFrame;
import game.data.entity.specific.Villager;
import packets.DataTypeProvider;
import packets.UUID;

/**
 * Handle the initial entity fields, we need to know the type before we can instantiate the correct object.
 */
public class PrimitiveEntity {
    protected int id;
    protected UUID uuid;
    protected int type;
    protected String typeName;

    protected static PrimitiveEntity parse(DataTypeProvider provider) {
        PrimitiveEntity ent = new PrimitiveEntity();

        ent.id = provider.readVarInt();
        ent.uuid = provider.readUUID();
        ent.type = provider.readVarInt();
        ent.typeName = WorldManager.getInstance().getEntityMap().getName(ent.type);

        return ent;
    }

    public Entity getEntity(Supplier<Entity> generate) {
        if (typeName == null) {
            return null;
        }

        if (typeName.endsWith("armor_stand")) {
            return moveTo(new ArmorStand());
        } else if (typeName.endsWith("item_frame")) {
            return moveTo(new ItemFrame());
        } else if (typeName.endsWith("villager")) {
            return moveTo(new Villager());
        } else if (typeName.endsWith("cat")) {
            return moveTo(new Cat());
        } else {
            return moveTo(generate.get());
        }
    }

    private Entity moveTo(Entity ent) {
        ent.id = this.id;
        ent.uuid = this.uuid;
        ent.type = this.type;
        ent.typeName = this.typeName;

        return ent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrimitiveEntity that = (PrimitiveEntity) o;

        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
