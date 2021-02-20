package game.data.entity;

import game.data.WorldManager;
import game.data.entity.specific.ArmorStand;
import game.data.entity.specific.ItemFrame;
import packets.DataTypeProvider;
import packets.UUID;

import java.util.function.Supplier;

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
}
