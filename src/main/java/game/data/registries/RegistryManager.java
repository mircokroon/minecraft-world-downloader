package game.data.registries;

import config.Config;
import game.data.chunk.BlockEntityRegistry;
import game.data.chunk.palette.BiomeRegistry;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import game.data.entity.specific.VillagerProfessionRegistry;
import game.data.entity.specific.VillagerTypeRegistry;

import java.io.IOException;

public class RegistryManager {
    private static RegistryManager instance;
    private MenuRegistry menuRegistry;
    private ItemRegistry itemRegistry;
    private BiomeRegistry biomeRegistry;
    private BlockEntityRegistry blockEntityRegistry;
    private VillagerProfessionRegistry villagerProfessionRegistry;
    private VillagerTypeRegistry villagerTypeRegistry;

    private RegistryManager() {
        if (Config.versionReporter().isAtLeast1_18()) {
            this.biomeRegistry = BiomeRegistry.create();
        }
    }

    public static RegistryManager getInstance() {
        if (instance == null) {
            instance = new RegistryManager();
        }
        return instance;
    }


    public MenuRegistry getMenuRegistry() {
        return menuRegistry;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    public BlockEntityRegistry getBlockEntityRegistry() {
        return blockEntityRegistry;
    }

    public VillagerProfessionRegistry getVillagerProfessionRegistry() {
        return villagerProfessionRegistry;
    }

    public VillagerTypeRegistry getVillagerTypeRegistry() {
        return villagerTypeRegistry;
    }


    public void setRegistries(RegistryLoader loader) throws IOException {
        this.menuRegistry = loader.generateMenuRegistry();
        this.itemRegistry = loader.generateItemRegistry();
        this.blockEntityRegistry = loader.generateBlockEntityRegistry();
        this.villagerProfessionRegistry = loader.generateVillagerProfessionRegistry();
        this.villagerTypeRegistry = loader.generateVillagerTypeRegistry();
    }
}
