package game.data.entity.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import game.data.WorldManager;
import game.data.container.Slot;
import game.data.entity.MobEntity;
import game.data.entity.metadata.MetaData_1_13;
import packets.DataTypeProvider;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.StringTag;

/**
 * Handle villagers because they have interesting metadata.
 */
public class Villager extends MobEntity {
    private VillagerMetaData metaData;
    private VillagerTradesMetaData villagerTradesMetadata;

    /**
     * Add additional fields needed for villagers.
     */
    @Override
    protected void addNbtData(CompoundTag root) {
        super.addNbtData(root);

        if (metaData != null) {
            metaData.addNbtTags(root);
        }

        if (villagerTradesMetadata != null) {
            villagerTradesMetadata.addNbtTags(root);
        }
    }

    @Override
    public void parseMetadata(DataTypeProvider provider) {
        if (metaData == null) {
            metaData = new VillagerMetaData();
        }
        try {
            metaData.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }

    public void parseTrades(DataTypeProvider provider) {
        if (villagerTradesMetadata == null) {
            villagerTradesMetadata = new VillagerTradesMetaData();
        }
        try {
            villagerTradesMetadata.parse(provider);
        } catch (Exception ex) {
            // couldn't parse metadata, whatever
        }
    }
}

class VillagerMetaData extends MetaData_1_13 {

    boolean noAI;
    int headShakeTimer;
    int type;
    int profession;
    int level;

    @Override
    public void addNbtTags(CompoundTag nbt) {
        super.addNbtTags(nbt);

        nbt.add("NoAI", new ByteTag(noAI ? 1 : 0));

        CompoundTag villagerData = new CompoundTag();

        String typeName = RegistryManager.getInstance().getVillagerTypeRegistry().getType(type);
        String professionName = RegistryManager.getInstance().getVillagerProfessionRegistry().getProfession(profession);
        villagerData.add("type", new StringTag(typeName));
        villagerData.add("profession", new StringTag(professionName));
        villagerData.add("level", new IntTag(level));

        nbt.add("VillagerData", villagerData);
    }

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        switch (i) {
            case 15:
                return provider -> noAI = (provider.readNext() & 0x01) > 0;
            case 17:
                return provider -> headShakeTimer = provider.readVarInt();
            case 18:
                return provider -> {
                    type = provider.readVarInt();
                    profession = provider.readVarInt();
                    level = provider.readVarInt();
                };
        }
        return super.getIndexHandler(i);
    }
}

class VillagerTradesMetaData {

    List<Trade> trades;
    int villagerLevel = 0;
    int villagerExp = 0;

    public void addNbtTags(CompoundTag nbt) {
        CompoundTag offers = new CompoundTag();
        offers.add("Recipes", null);

        nbt.add("Offers", offers);
    }

    public void parse(DataTypeProvider provider) {
        trades = new ArrayList<>();

        provider.readVarInt(); // Window ID
        byte numberofTrades = provider.readNext(); // number of trades
        for (byte i = 0; i < numberofTrades; i++) {
            Slot firstItemSlot = provider.readSlot();
            Slot itemToReceive = provider.readSlot();
            boolean hasSecondItem = provider.readBoolean();

            Slot secondItem = null;
            if (hasSecondItem) {
                secondItem = provider.readSlot();
            }
            provider.readBoolean(); // Trade disabled
            int uses = provider.readInt();
            int maxUses = provider.readInt();
            int xp = provider.readInt();
            int specialPrice = provider.readInt();
            float priceMultiplier = provider.readFloat();
            int demand = provider.readInt();

            trades.add(new Trade(firstItemSlot, secondItem, itemToReceive, demand, maxUses, priceMultiplier,
                    specialPrice, uses, xp));
        }
        villagerLevel = provider.readVarInt(); // Current villager level
        villagerExp = provider.readVarInt(); // Total experience for this villager
        provider.readBoolean(); // Is regular villager
        provider.readBoolean(); // Can restock
    }

    record Trade(Slot firstItemSlot2, Slot secondItem2, Slot itemToReceive2, int demand, int maxUses,
            float priceMultiplier, int specialPrice, int uses, int xp) {
    }
}