package game.data.entity;

import config.Config;
import game.data.container.Slot;
import game.data.WorldManager;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.entity.version.EquipmentReader;
import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class Entity extends PrimitiveEntity implements IMovableEntity {
    private static EquipmentReader equipmentReader;

    public final static double CHANGE_MULTIPLIER = 4096.0;
    public final static float ROTATION_MULTIPLIER = 360f / 256f;

    protected double x, y, z;
    private CoordinateDim2D position;
    private Dimension dimension;

    protected float pitch;
    protected float yaw;
    private Slot[] equipment;

    private BiConsumer<CoordinateDim2D, CoordinateDim2D> onMove;

    Entity() {
        if (equipmentReader == null) {
            equipmentReader = EquipmentReader.getVersioned();
        }
        this.dimension = WorldManager.getInstance().getDimension();
    }

    public SpecificTag toNbt() {
        CompoundTag root = new CompoundTag();

        addPosition(root);

        // write velocity as 0, we'd rather have entities become stationary
        List<DoubleTag> motion = Arrays.asList(new DoubleTag(0), new DoubleTag(0), new DoubleTag(0));
        root.add("Motion", new ListTag(ListTag.TAG_DOUBLE, motion));

        root.add("UUIDLeast", new LongTag(uuid.getLower()));
        root.add("UUIDMost", new LongTag(uuid.getUpper()));
        root.add("id", new StringTag(typeName));

        List<FloatTag> pos = Arrays.asList(new FloatTag(angleToRotation(yaw)), new FloatTag(angleToRotation(pitch)));
        root.add("Rotation", new ListTag(ListTag.TAG_FLOAT, pos));

        addNbtData(root);
        addNbtEquipment(root);

        return root;
    }

    /**
     * First two slots are the entity's hands, the other four are armor slots.
     */
    private void addNbtEquipment(CompoundTag root) {
        if (equipment == null) {
            return;
        }

        ListTag handItems = new ListTag(Tag.TAG_COMPOUND, Arrays.asList(
                slotToNbt(equipment[0]),
                slotToNbt(equipment[1])
        ));
        ListTag armorItems = new ListTag(Tag.TAG_COMPOUND, Arrays.asList(
                slotToNbt(equipment[2]),
                slotToNbt(equipment[3]),
                slotToNbt(equipment[4]),
                slotToNbt(equipment[5])
        ));

        root.add("HandItems", handItems);
        root.add("ArmorItems", armorItems);
    }

    private CompoundTag slotToNbt(Slot s) {
        if (s == null) {
            return new CompoundTag();
        }
        return s.toNbt();
    }

    protected float angleToRotation(float angle) {
        float rotation = angle * ROTATION_MULTIPLIER;
        if (rotation < 0) {
            rotation += 360;
        }
        return rotation;
    }

    protected void addPosition(CompoundTag nbt) {
        double offsetX = x - Config.getCenterX();
        double offsetZ = z - Config.getCenterZ();
        List<DoubleTag> pos = Arrays.asList(new DoubleTag(offsetX), new DoubleTag(y), new DoubleTag(offsetZ));
        nbt.add("Pos", new ListTag(ListTag.TAG_DOUBLE, pos));
    }

    public void parseMetadata(DataTypeProvider provider) { };

    protected abstract void addNbtData(CompoundTag root);

    /**
     * velocity is not saved but we still read it
     */
    protected static void parseVelocity(DataTypeProvider provider) {
        int velX = provider.readShort();
        int velY = provider.readShort();
        int velZ = provider.readShort();
    }

    public Integer getId() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void registerOnLocationChange(BiConsumer<CoordinateDim2D, CoordinateDim2D> handler) {
        this.onMove = handler;
        handler.accept(null, position.addDimension(dimension));
    }

    private void updateCoordinate() {
        CoordinateDim2D newPos = new CoordinateDim2D((int) Math.round(x), (int) Math.round(z), dimension);
        if (this.onMove != null) {
            this.onMove.accept(this.position, newPos);
        }
        this.position = newPos;
    }

    public void incrementPosition(int dx, int dy, int dz) {
        this.x += dx / CHANGE_MULTIPLIER;
        this.y += dy / CHANGE_MULTIPLIER;
        this.z += dz / CHANGE_MULTIPLIER;
        updateCoordinate();
    }

    public void readPosition(DataTypeProvider provider) {
        this.x = provider.readDouble();
        this.y = provider.readDouble();
        this.z = provider.readDouble();

        updateCoordinate();
    }

    /**
     * Parse list of slot data. Each slot starts with a byte where the first bit indicates whether another slot will
     * follow, the other 7 indicate the slot index.
     */
    public void addEquipment(DataTypeProvider provider) {
        this.equipment = equipmentReader.readSlots(this.equipment, provider);
    }
}
