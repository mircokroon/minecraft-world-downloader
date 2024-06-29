package game.data.villagers;

import config.Config;
import config.Version;
import game.data.entity.EntityRegistry;
import game.data.entity.IMovableEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.data.WorldManager;
import game.data.container.Slot;
import game.data.coordinates.CoordinateDim3D;
import game.data.entity.specific.Villager;
import packets.DataTypeProvider;
import packets.UUID;
import packets.builder.Chat;
import packets.builder.MessageTarget;
import packets.builder.PacketBuilder;

public class VillagerManager {

    private final Map<Integer, VillagerData> knownTrades = new HashMap<>();
    private final Map<UUID, VillagerData> storedVillager = new HashMap<>();

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
            lastInteractedLocation = lastInteractedWith.getCoordinate3D().addDimension3D(WorldManager.getInstance().getDimension());
        }
    }
    
    public void parseAndStoreVillagerTrade(DataTypeProvider provider) {
        // TODO: villager trades cannot be saved since readSlot is broken in 1.20.2+ due to the
        //  new item components
        if (Config.versionReporter().isAtLeast(Version.V1_20_2)) {
            return;
        }

        if (lastInteractedWith == null) {
            return; // This should be impossible
        }

        List<VillagerTrade> trades = new ArrayList<>();

        int windowId = provider.readVarInt(); // Window ID

        int numberOfTrades;

        if (Config.versionReporter().isAtLeast(Version.V1_19)) {
            numberOfTrades = provider.readVarInt();
        } else {
            numberOfTrades = provider.readNext();
        }

        for (byte i = 0; i < numberOfTrades; i++) {
            Slot firstItem = provider.readSlot();
            Slot receivedItem = provider.readSlot();

            Slot secondItem = null;
            
            if (Config.versionReporter().isAtLeast(Version.V1_19_3) || provider.readBoolean()) {
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
        knownTrades.put(windowId, new VillagerData(trades, villagerLevel, villagerExp, lastInteractedLocation));
    }

    public void closeWindow(int windowId) {
        if (!knownTrades.containsKey(windowId)) {
            return;
        }
        final VillagerData villagerData = knownTrades.remove(windowId);
        storedVillager.put(lastInteractedWith.getUUID(), villagerData);

        if (Config.sendInfoMessages()) {
            if (villagerData.trades().size() > 0) {
                String message = "Stored villager trade at " + lastInteractedLocation + ", with " + villagerData.trades.size() + " trade(s)";
                Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
            } else {
                Chat message = new Chat("No villager trade at " + lastInteractedLocation);
                message.setColor("red");
                Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
            }
        }
    }

    public void loadPreviousTradeAt(Villager villager) {
        if (storedVillager.containsKey(villager.getUUID())){
            VillagerData data = storedVillager.get(villager.getUUID());
            villager.updateTrades(data.trades(), data.villagerLevel(), data.villagerExp(), data.lastLocation());
        }
    }

    private enum InteractionType {
        INTERACT(0), ATTACK(1), INTERACT_AT(2);

        final int index;

        InteractionType(int type) {
            this.index = type;
        }
    }

    private record VillagerData(List<VillagerTrade> trades,
                                int villagerLevel,
                                int villagerExp,
                                CoordinateDim3D lastLocation) {
    }

}


