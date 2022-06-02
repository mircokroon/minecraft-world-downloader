package game.data.villagers;

import game.data.container.Slot;

public record VillagerTrade(Slot firstItem, Slot secondItem, Slot sellingItem, int demand, int maxUses,
        float priceMultiplier, int specialPrice, int uses, int xp) {
}