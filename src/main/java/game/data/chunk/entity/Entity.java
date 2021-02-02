package game.data.chunk.entity;

import game.data.Coordinate3D;
import game.data.WorldManager;
import packets.DataTypeProvider;
import packets.UUID;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.DoubleTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.LongTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;

import java.util.List;

public abstract class Entity {
    private int id;
    private UUID uuid;
    private int type;
    private String typeName;

    private double x, y, z;
    private int velX, velY, velZ;

    Entity() { }

    protected static Entity parseEntity(DataTypeProvider provider, Entity ent) {
        ent.id = provider.readVarInt();

        ent.uuid = provider.readUUID();
        ent.type = provider.readVarInt();
        ent.typeName = WorldManager.getInstance().getEntityMap().getName(ent.type);

        // unknown entity - don't bother parsing the rest
        if (ent.typeName == null) {
            return null;
        }

        ent.parsePosition(provider);
        ent.parseRotation(provider);
        ent.parseVelocity(provider);
        ent.parseMetadata(provider);

        return ent;
    }



    public SpecificTag toNbt() {
        CompoundTag root = new CompoundTag();

        List<DoubleTag> pos = List.of(new DoubleTag(x), new DoubleTag(y), new DoubleTag(z));
        root.add("Pos", new ListTag(ListTag.TAG_DOUBLE, pos));

        List<DoubleTag> motion = List.of(new DoubleTag(velX), new DoubleTag(velY), new DoubleTag(velZ));
        root.add("Motion", new ListTag(ListTag.TAG_DOUBLE, motion));

        root.add("UUIDLeast", new LongTag(uuid.getLower()));
        root.add("UUIDMost", new LongTag(uuid.getUpper()));
        root.add("id", new StringTag(typeName));

        addNbtData(root);
        return root;
    }

    public void parseMetadata(DataTypeProvider provider) { };

    protected abstract void addNbtData(CompoundTag root);

    protected abstract void parseRotation(DataTypeProvider provider);

    private void parsePosition(DataTypeProvider provider) {
        this.x = provider.readDouble();
        this.y = provider.readDouble();
        this.z = provider.readDouble();
    }

    private void parseVelocity(DataTypeProvider provider) {
        this.velX = provider.readShort();
        this.velY = provider.readShort();
        this.velZ = provider.readShort();
    }

    public Coordinate3D getPosition() {
        return new Coordinate3D(x, y, z);
    }

    public Integer getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }
}
