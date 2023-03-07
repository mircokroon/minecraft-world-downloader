package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import packets.UUID;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;

/**
 * Handle cats as they have cat type metadata.
 */
public class Cat extends MobEntity {
    private CatMetaData metaData;

    /**
     * Add additional fields needed for cats.
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
            metaData = new CatMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    private class CatMetaData extends MetaData_1_19_3 {

        int type = 1;
        int collarColor = 14;

        boolean isSitting = false;
        UUID owner = null;

        @Override
        public void addNbtTags(CompoundTag nbt) {
            super.addNbtTags(nbt);

            nbt.add("CatType", new IntTag(type));
            nbt.add("CollarColor", new ByteTag(collarColor));
            if(owner != null) {
                nbt.add("Owner", new IntArrayTag(owner.asIntArray()));
            }
            nbt.add("Sitting", new ByteTag(isSitting ? 1 : 0));
        }

        @Override
        public Consumer<DataTypeProvider> getIndexHandler(int i) {
            return switch (i) {
                case 2 -> provider -> collarColor = provider.readVarInt();
                case 17 -> provider -> {
                    byte next = provider.readNext();
                    isSitting = (next & 0x01) > 0;
                };
                case 18 -> provider -> owner = provider.readOptUUID();
                case 19 -> provider -> type = provider.readVarInt();
                default -> super.getIndexHandler(i);
            };
        }
    }
}
