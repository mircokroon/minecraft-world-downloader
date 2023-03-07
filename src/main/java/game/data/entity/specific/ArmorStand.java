package game.data.entity.specific;

import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import se.llbit.nbt.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handle Armorstands because many servers will use them for decorations.
 */
public class ArmorStand extends MobEntity {
    private ArmorStandMetaData metaData;

    /**
     * Add additional fields needed for armor stands.
     */
    @Override
    protected void addNbtData(CompoundTag root) {
        super.addNbtData(root);

        if (metaData != null) {
            metaData.addNbtTags(root);
        }
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

class ArmorStandMetaData extends MetaData_1_19_3 {
    private boolean isSmall;
    private boolean hasArms = true;
    private boolean hasNoBasePlate;
    private boolean isMarker;

    Map<String, Rotation> pose;

    public ArmorStandMetaData() {
        pose = new HashMap<>();
    }

    @Override
    public void addNbtTags(CompoundTag nbt) {
        super.addNbtTags(nbt);

        nbt.add("Small", new ByteTag(isSmall ? 1 : 0));
        nbt.add("ShowArms", new ByteTag(hasArms ? 1 : 0));
        nbt.add("NoBasePlate", new ByteTag(hasNoBasePlate ? 1 : 0));
        nbt.add("Marker", new ByteTag(isMarker ? 1 : 0));

        // for armorstands we always want to disable gravity
        nbt.add("NoGravity", new ByteTag(1));

        CompoundTag pose = new CompoundTag();
        this.pose.forEach((name, rotation) -> pose.add(name, rotation.toNbt()));

        nbt.add("Pose", pose);
    }

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        return switch (i) {
            case 14 -> provider -> {
                byte status = provider.readNext();
                isSmall = (status & 0x01) > 0;
                hasArms = (status & 0x04) > 0;
                hasNoBasePlate = (status & 0x08) > 0;
                isMarker = (status & 0x10) > 0;
            };
            case 15 -> provider -> pose.put("Head", Rotation.read(provider));
            case 16 -> provider -> pose.put("Body", Rotation.read(provider));
            case 17 -> provider -> pose.put("LeftArm", Rotation.read(provider));
            case 18 -> provider -> pose.put("RightArm", Rotation.read(provider));
            case 19 -> provider -> pose.put("LeftLeg", Rotation.read(provider));
            case 20 -> provider -> pose.put("RightLeg", Rotation.read(provider));
            default -> super.getIndexHandler(i);
        };
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
}