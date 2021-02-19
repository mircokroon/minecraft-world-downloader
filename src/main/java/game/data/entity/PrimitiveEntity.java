package game.data.entity;

import game.data.WorldManager;
import game.data.entity.specific.ArmorStand;
import game.data.entity.specific.ItemFrame;
import packets.DataTypeProvider;
import packets.UUID;

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

    public Entity getLivingEntity() {
        if (typeName == null) {
            return null;
        }
        return moveTo(new MobEntity());
    }

    public Entity getObjectEntity() {
        if (typeName == null) {
            return null;
        }

        if (typeName.endsWith("item_frame")) {
            return moveTo(new ItemFrame());
        } else if (typeName.endsWith("armor_stand")) {
            return moveTo(new ArmorStand());
        } else {
            return moveTo(new ObjectEntity());
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
