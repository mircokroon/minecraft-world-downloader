package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.container.Slot;
import game.data.entity.ObjectEntity;
import game.data.entity.metadata.MetaData_1_19_3;
import packets.DataTypeProvider;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ShortTag;

/**
 * Handle dropped items
 */
public class DroppedItem extends ObjectEntity {
    private ItemMetaData metaData;

    public DroppedItem() {
        super();
    }

    /**
     * Add additional fields needed for dropped items.
     */
    @Override
    protected void addNbtData(CompoundTag root) {
        super.addNbtData(root);

        // TODO: make option in menu for whether items should despawn or stay permanently
        root.add("Age", new ShortTag((short) -32768)); // Default age: 6000. Set to -32768 to never despawn
        root.add("Health", new ShortTag((short) 5)); // Default health

        if (metaData != null) {
            metaData.addNbtTags(root);
        }
    }

    @Override
    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = new ItemMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    private class ItemMetaData extends MetaData_1_19_3 {
        Slot item;

        @Override
        public void addNbtTags(CompoundTag nbt) {
            if (item != null) {
                nbt.add("Item", item.toNbt());
            }
        }

        @Override
        public Consumer<DataTypeProvider> getIndexHandler(int i) {
            switch (i) {
                case 8: return provider -> item = provider.readSlot();
            }
            return super.getIndexHandler(i);
        }
    }
}