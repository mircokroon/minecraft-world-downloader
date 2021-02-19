package game.data.entity;

import config.Config;
import game.data.coordinates.Coordinate3D;
import game.data.WorldManager;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import packets.DataTypeProvider;
import packets.UUID;
import se.llbit.nbt.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class Entity extends PrimitiveEntity {
    private final static double CHANGE_MULTIPLIER = 4096.0;
    private final static float ROTATION_MULTIPLIER = 360f / 256f;

    protected double x, y, z;
    protected int velX, velY, velZ;
    private CoordinateDim2D position;
    private Dimension dimension;

    protected float pitch;
    protected float yaw;

    private BiConsumer<CoordinateDim2D, CoordinateDim2D> onMove;

    Entity() {
        this.dimension = WorldManager.getInstance().getDimension();
    }

    public SpecificTag toNbt() {
        CompoundTag root = new CompoundTag();

        addPosition(root);

        List<DoubleTag> motion = Arrays.asList(new DoubleTag(velX), new DoubleTag(velY), new DoubleTag(velZ));
        root.add("Motion", new ListTag(ListTag.TAG_DOUBLE, motion));

        root.add("UUIDLeast", new LongTag(uuid.getLower()));
        root.add("UUIDMost", new LongTag(uuid.getUpper()));
        root.add("id", new StringTag(typeName));

        List<FloatTag> pos = Arrays.asList(new FloatTag(angleToRotation(yaw)), new FloatTag(angleToRotation(pitch)));
        root.add("Rotation", new ListTag(ListTag.TAG_FLOAT, pos));

        addNbtData(root);

        return root;
    }

    private float angleToRotation(float angle) {
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

    protected void parseRotation(DataTypeProvider provider) {
        this.pitch = provider.readNext();
        this.yaw = provider.readNext();
    }

    public void parsePosition(DataTypeProvider provider) {
        this.x = provider.readDouble();
        this.y = provider.readDouble();
        this.z = provider.readDouble();
        updateCoordinate();
    }

    protected void parseVelocity(DataTypeProvider provider) {
        this.velX = provider.readShort();
        this.velY = provider.readShort();
        this.velZ = provider.readShort();
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

    public String toSimpleString() {
        return typeName + "@(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", type=" + type +
                ", typeName='" + typeName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", velX=" + velX +
                ", velY=" + velY +
                ", velZ=" + velZ +
                ", position=" + position +
                ", dimension=" + dimension +
                ", onMove=" + onMove +
                '}';
    }

    protected void parseFully(DataTypeProvider provider) {
        parsePosition(provider);
        parseRotation(provider);
    }
}
