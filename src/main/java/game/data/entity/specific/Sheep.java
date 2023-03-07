package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;

/**
 * Handle sheep as they have sheep type metadata.
 */
public class Sheep extends MobEntity {
    private SheepMetaData metaData;

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
            metaData = new SheepMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    private class SheepMetaData extends MetaData_1_19_3 {

        byte colorID = 0;
        boolean isSheared = false;

        @Override
        public void addNbtTags(CompoundTag nbt) {
            super.addNbtTags(nbt);

            nbt.add("Color", new ByteTag(colorID));
            nbt.add("Sheared", new ByteTag(isSheared ? 1 : 0));
        }
        @Override
        public Consumer<DataTypeProvider> getIndexHandler(int i) {
            if (i == 17) {
                return provider -> {
                    byte flags = provider.readNext();
                    colorID = (byte) (flags & 0x0F);
                    isSheared = (flags & 0x10) > 0;
                };
            }
            return super.getIndexHandler(i);
        }
    }
}
