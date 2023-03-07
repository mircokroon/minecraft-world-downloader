package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.container.Slot;
import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import packets.UUID;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.StringTag;

/**
 * Handle sheep as they have sheep type metadata.
 */
public class Horse extends MobEntity {
    private HorseMetaData metaData;

    /**
     * Add additional fields needed for sheep.
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
            metaData = new HorseMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    private class HorseMetaData extends MetaData_1_19_3 {

        int variant = 0;
        UUID owner = null;

        boolean isTame = false;
        boolean isSaddled = false;

        @Override
        public void addNbtTags(CompoundTag nbt) {
            super.addNbtTags(nbt);

            nbt.add("Variant", new IntTag(variant));
            if (owner != null) {
                nbt.add("Owner", new IntArrayTag(owner.asIntArray()));
            }
            nbt.add("Tame", new ByteTag(isTame ? 1 : 0));

            if (isSaddled) {
                Slot saddle = new Slot("minecraft:saddle", (byte) 1);
                nbt.add("SaddleItem", saddle.toNbt(0));
            }
        }

        @Override
        public Consumer<DataTypeProvider> getIndexHandler(int i) {
            return switch (i) {
                case 17 -> provider -> {
                    byte flags = provider.readNext();
                    isTame = (flags & 0x02) > 0;
                    isSaddled = (flags & 0x04) > 0;
                };
                case 18 -> provider -> owner = provider.readOptUUID();
                case 19 -> provider -> variant = provider.readVarInt();
                default -> super.getIndexHandler(i);
            };
        }
    }
}
