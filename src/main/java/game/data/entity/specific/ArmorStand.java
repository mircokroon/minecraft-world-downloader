package game.data.entity.specific;

import game.data.entity.MobEntity;
import game.data.entity.ObjectEntity;
import game.data.entity.metadata.MetaData_1_13;
import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ArmorStand extends MobEntity {
    private ArmorStandMetaData metaData;

    /**
     * Add additional fields needed to render item frames.
     */
    @Override
    protected void addNbtData(CompoundTag root) {
        super.addNbtData(root);

        root.add("Small", new ByteTag(metaData.isSmall ? 1 : 0));
        root.add("ShowArms", new ByteTag(metaData.hasArms ? 1 : 0));
        root.add("NoBasePlate", new ByteTag(metaData.hasNoBasePlate ? 1 : 0));
        root.add("Marker", new ByteTag(metaData.isMarker ? 1 : 0));

        CompoundTag pose = new CompoundTag();
        metaData.pose.forEach((name, rotation) -> pose.add(name, rotation.toNbt()));

        root.add("Pose", pose);
    }

    @Override
    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = new ArmorStandMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }
}

class ArmorStandMetaData extends MetaData_1_13 {
    boolean isSmall;
    boolean hasArms = true;
    boolean hasNoBasePlate;
    boolean isMarker;

    Map<String, Rotation> pose;

    public ArmorStandMetaData() {
        pose = new HashMap<>();
    }

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        switch (i) {
            case 14: return provider -> {
                byte status = provider.readNext();
                isSmall = (status & 0x01) > 0;
                hasArms = (status & 0x04) > 0;
                hasNoBasePlate = (status & 0x08) > 0;
                isMarker = (status & 0x10) > 0;
            };
            case 15: return provider -> pose.put("Head", Rotation.read(provider));
            case 16: return provider -> pose.put("Body", Rotation.read(provider));
            case 17: return provider -> pose.put("LeftArm", Rotation.read(provider));
            case 18: return provider -> pose.put("RightArm", Rotation.read(provider));
            case 19: return provider -> pose.put("LeftLeg", Rotation.read(provider));
            case 20: return provider -> pose.put("RightLeg", Rotation.read(provider));
        }
        return super.getIndexHandler(i);
    }
}

class Rotation {
    float x;
    float y;
    float z;

    public Rotation(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Rotation read(DataTypeProvider provider) {
        return new Rotation(provider.readFloat(), provider.readFloat(), provider.readFloat());
    }

    public ListTag toNbt() {
        return new ListTag(Tag.TAG_FLOAT, Arrays.asList(
                new FloatTag(x),
                new FloatTag(y),
                new FloatTag(z)
        ));
    }

    @Override
    public String toString() {
        return "Rotation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}