package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;

/**
 * Handle axolotls as they have axolotl type metadata.
 */
public class Axolotl extends MobEntity {
    private AxolotlMetaData metaData;

    /**
     * Add additional fields needed for axolotls.
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
            metaData = new AxolotlMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    private class AxolotlMetaData extends MetaData_1_19_3 {

        int variant = 0;
        boolean wasSpawnedFromBucket = false;

        @Override
        public void addNbtTags(CompoundTag nbt) {
            super.addNbtTags(nbt);

            nbt.add("FromBucket", new ByteTag(wasSpawnedFromBucket ? 1 : 0));
            nbt.add("Variant", new IntTag(variant));
        }
        @Override
        public Consumer<DataTypeProvider> getIndexHandler(int i) {
            return switch (i) {
                case 17 -> provider -> variant = provider.readVarInt();
                case 19 -> provider -> wasSpawnedFromBucket = provider.readBoolean();
                default -> super.getIndexHandler(i);
            };
        }
    }
}