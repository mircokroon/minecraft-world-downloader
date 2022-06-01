package game.data.villagers;

import java.util.ArrayList;
import java.util.List;

import game.data.WorldManager;
import game.data.container.Slot;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim3D;
import game.data.entity.Entity;
import game.data.entity.specific.Villager;
import packets.DataTypeProvider;

public class VillagerManager {

    private Villager lastInteractedWith;
    private CoordinateDim3D lastInteractedLocation;
    private List<VillagerTrade> trades;
    int villagerLevel = 0;
    int villagerExp = 0;

    public void lastInteractedWith(DataTypeProvider provider) {
        Entity entity = WorldManager.getInstance().getEntityRegistry().getEntity(provider.readVarInt());
        if(entity == null || !(entity instanceof Villager)) {
            return;
        }

        int interactionType = provider.readVarInt(); // Interact type
        if(interactionType == 2) {
            lastInteractedWith = (Villager) entity;
            float x = provider.readFloat();
            float y = provider.readFloat();
            float z = provider.readFloat();
            lastInteractedLocation = new Coordinate3D(x, y, z).addDimension3D(WorldManager.getInstance().getDimension());
        }
    }
    
    public void parseAndStoreVillagerTrade(DataTypeProvider provider) {
        if(lastInteractedWith == null) {
            return; // This should be impossible
        }

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

            trades.add(new VillagerTrade(firstItemSlot, secondItem, itemToReceive, demand, maxUses, priceMultiplier,
                    specialPrice, uses, xp));
        }
        villagerLevel = provider.readVarInt(); // Current villager level
        villagerExp = provider.readVarInt(); // Total experience for this villager
        provider.readBoolean(); // Is regular villager
        provider.readBoolean(); // Can restock

        lastInteractedWith.updateTrades(trades, villagerLevel, villagerExp, lastInteractedLocation);
    }
}
