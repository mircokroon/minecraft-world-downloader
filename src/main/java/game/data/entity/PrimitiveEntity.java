package game.data.entity;

import config.Config;
import config.VersionReporter;
import java.util.function.Supplier;

import game.data.WorldManager;
import game.data.entity.specific.ArmorStand;
import game.data.entity.specific.Axolotl;
import game.data.entity.specific.Cat;
import game.data.entity.specific.Horse;
import game.data.entity.specific.DroppedItem;
import game.data.entity.specific.ItemFrame;
import game.data.entity.specific.Sheep;
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

        if (!Config.versionReporter().isAtLeast1_13()) {
            return null;
        }

        return moveTo(switch(typeName) {
            case "minecraft:armor_stand" -> new ArmorStand();
            case "minecraft:axolotl" -> new Axolotl();
            case "minecraft:cat" -> new Cat();
            case "minecraft:horse" -> new Horse();
            case "minecraft:item" -> new DroppedItem();
            case "minecraft:item_frame", "minecraft:glow_item_frame" -> new ItemFrame();
            case "minecraft:sheep" -> new Sheep();
            case "minecraft:villager" -> new Villager();
            default -> generate.get();
        });
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
