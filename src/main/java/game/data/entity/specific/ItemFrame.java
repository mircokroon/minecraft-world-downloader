package game.data.entity.specific;

import game.data.container.Slot;
import game.data.entity.ObjectEntity;
import game.data.entity.metadata.MetaData_1_13;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;

import java.util.function.Consumer;

public class ItemFrame extends ObjectEntity {
    int facing;
    private ItemFrameMetaData metaData;

    public ItemFrame() {
        super();
    }

    /**
     * Add additional fields needed to render item frames.
     */
    @Override
    protected void addNbtData(CompoundTag root) {
        super.addNbtData(root);
        root.add("Item", metaData.item.toNbt());
        root.add("ItemRotation", new IntTag(metaData.rotation));
        root.add("Facing", new IntTag(facing));

        root.add("TileX", new IntTag((int) x));
        root.add("TileY", new IntTag((int) y));
        root.add("TileZ", new IntTag((int) z));

        // prevent floating item frames from popping off
        root.add("Fixed", new ByteTag(1));
        root.add("Invisible", new ByteTag(metaData.isInvisible ? 1 : 0));
    }

    @Override
    protected void parseData(DataTypeProvider provider) {
        facing = provider.readInt();
    }

    @Override
    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = new ItemFrameMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }
}


class ItemFrameMetaData extends MetaData_1_13 {
    Slot item;
    int rotation;
    boolean isInvisible;

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        switch (i) {
            case 0: return provider -> isInvisible = (provider.readNext() & 0x20) > 0;
            case 7: return provider -> item = provider.readSlot();
            case 8: return provider -> rotation = provider.readVarInt();
        }
        return super.getIndexHandler(i);
    }
}