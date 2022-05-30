package game.data.entity.specific;

import java.util.function.Consumer;

import game.data.WorldManager;
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

    /**
     * Add additional fields needed for villagers.
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
            metaData = new VillagerMetaData();
        }
        try {
            metaData.parse(provider);
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
        villagerData.add("type", new StringTag(WorldManager.getInstance().getVillagerTypeMap().getType(type)));
        villagerData.add("profession", new StringTag(WorldManager.getInstance().getVillagerProfessionMap().getProfession(profession)));
        villagerData.add("level", new IntTag(level));

        nbt.add("VillagerData", villagerData);
    }

    @Override
    public Consumer<DataTypeProvider> getIndexHandler(int i) {
        switch (i) {
            case 15: return provider -> noAI = (provider.readNext() & 0x01) > 0;
            case 17: return provider -> headShakeTimer = provider.readVarInt();
            case 18: return provider -> {
                type = provider.readVarInt();
                profession = provider.readVarInt();
                level = provider.readVarInt();
            };
        }
        return super.getIndexHandler(i);
    }
}