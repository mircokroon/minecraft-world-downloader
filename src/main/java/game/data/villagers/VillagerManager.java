package game.data.villagers;

import config.Config;
import game.data.entity.EntityRegistry;
import game.data.entity.IMovableEntity;
import java.util.ArrayList;
import java.util.List;

import game.data.WorldManager;
import game.data.container.Slot;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim3D;
import game.data.entity.specific.Villager;
import packets.DataTypeProvider;

public class VillagerManager {
    private Villager lastInteractedWith;
    private CoordinateDim3D lastInteractedLocation;

    public void lastInteractedWith(DataTypeProvider provider) {
        EntityRegistry registry = WorldManager.getInstance().getEntityRegistry();
        IMovableEntity entity = registry.getMovableEntity(provider.readVarInt());

        if (!(entity instanceof Villager)) {
            return;
        }

        int interactionType = provider.readVarInt();
        if (interactionType == InteractionType.INTERACT_AT.index) {
            lastInteractedWith = (Villager) entity;
            float x = provider.readFloat();
            float y = provider.readFloat();
            float z = provider.readFloat();
            lastInteractedLocation = new Coordinate3D(x, y, z).addDimension3D(WorldManager.getInstance().getDimension());
        }
    }
    
    public void parseAndStoreVillagerTrade(DataTypeProvider provider) {
        if (lastInteractedWith == null) {
            return; // This should be impossible
        }

        List<VillagerTrade> trades = new ArrayList<>();

        provider.readVarInt(); // Window ID
        byte numberOfTrades = provider.readNext();
        for (byte i = 0; i < numberOfTrades; i++) {
            Slot firstItem = provider.readSlot();
            Slot receivedItem = provider.readSlot();

            Slot secondItem = null;
            
            if (Config.versionReporter().isAtLeast1_19_3() || provider.readBoolean()) {
                secondItem = provider.readSlot();
            }

            provider.readBoolean(); // Trade disabled
            int uses = provider.readInt();
            int maxUses = provider.readInt();
            int xp = provider.readInt();
            int specialPrice = provider.readInt();
            float priceMultiplier = provider.readFloat();
            int demand = provider.readInt();

            trades.add(new VillagerTrade(
                    firstItem, secondItem, receivedItem, demand, maxUses, priceMultiplier, specialPrice, uses, xp
            ));
        }
        int villagerLevel = provider.readVarInt();
        int villagerExp = provider.readVarInt();
        provider.readBoolean(); // Is regular villager
        provider.readBoolean(); // Can restock

        lastInteractedWith.updateTrades(trades, villagerLevel, villagerExp, lastInteractedLocation);
    }

    private enum InteractionType {
        INTERACT(0), ATTACK(1), INTERACT_AT(2);

        final int index;

        InteractionType(int type) {
            this.index = type;
        }
    }

}


