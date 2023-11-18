package game.data.entity;

import static util.ExceptionHandling.attempt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.coordinates.CoordinateDim2D;
import game.data.entity.specific.Villager;
import packets.DataTypeProvider;
import se.llbit.nbt.SpecificTag;

public class EntityRegistry {

    private final Map<Integer, PlayerEntity> players;
    private final Map<CoordinateDim2D, Set<Entity>> perChunk;
    private final Map<Integer, Entity> entities;
    private final WorldManager worldManager;

    private final ExecutorService executor;

    public EntityRegistry(WorldManager manager) {
        this.worldManager = manager;
        this.perChunk = new ConcurrentHashMap<>();
        this.entities = new ConcurrentHashMap<>();
        this.players = new ConcurrentHashMap<>();

        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Entity Parser Service"));;
    }
    /**
     * Add a new entity.
     */
    public void addEntity(DataTypeProvider provider, Function<DataTypeProvider, Entity> parser) {
        this.executor.execute(() -> attempt(() -> {
            Entity ent = parser.apply(provider);
            if (ent == null) { return; }
            entities.put(ent.getId(), ent);

            ent.registerOnLocationChange((oldPos, newPos) -> {
                CoordinateDim2D oldChunk = oldPos == null ? null : oldPos.globalToDimChunk();
                CoordinateDim2D newChunk = newPos.globalToDimChunk();

                // if they're the same, just mark the chunk as unsaved
                if (oldPos == newPos) {
                    markUnsaved(newChunk);
                    return;
                }

                Set<Entity> entities = oldChunk == null ? null : perChunk.get(oldChunk);
                if (entities != null) {
                    entities.remove(ent);

                    if (entities.isEmpty()) {
                        perChunk.remove(oldChunk);
                    }
                }

                Set<Entity> set = perChunk.computeIfAbsent(newChunk, (k) -> ConcurrentHashMap.newKeySet());
                set.add(ent);

                markUnsaved(newChunk);

            });
            
            if (ent instanceof Villager villager) {
                WorldManager.getInstance().getVillagerManager().loadPreviousTradeAt(villager);
                villager.registerOnTradeUpdate((pos) -> markUnsaved(pos.globalToDimChunk()));
            }
        }));
    }

    public void addPlayer(DataTypeProvider provider) {
        executor.execute(() -> attempt(() -> {
            int entId = provider.readVarInt();
            players.put(entId, PlayerEntity.parse(provider));
        }));
    }

    private void markUnsaved(CoordinateDim2D coord) {
        Chunk chunk = worldManager.getChunk(coord);
        if (chunk != null) {
            worldManager.touchChunk(chunk);
        }
    }

    /**
     * Delete all tile entities for a chunk, only done when the chunk is also unloaded. Note that this only related to
     * tile entities sent in the update-tile-entity packets, ones sent with the chunk will only be stored in the chunk.
     * @param location the position of the chunk for which we can delete tile entities.
     */
    public void unloadChunk(CoordinateDim2D location) {
        Set<Entity> entities = perChunk.remove(location);
        if (entities == null) { return; }

        for (Entity e : entities) {
            this.entities.remove(e.getId());
        }
    }


    public void addMetadata(DataTypeProvider provider) {
        this.executor.execute(() -> attempt(() -> {
            Entity ent = entities.get(provider.readVarInt());

            if (ent != null) {
                ent.parseMetadata(provider);
            }
        }));
    }

    public void updatePositionRelative(DataTypeProvider provider) {
        this.executor.execute(() -> attempt(() -> {
            IMovableEntity ent = getMovableEntity(provider.readVarInt());

            if (ent != null) {
                ent.incrementPosition(provider.readShort(), provider.readShort(), provider.readShort());
            }
        }));
    }

    public void updatePositionAbsolute(DataTypeProvider provider) {
        this.executor.execute(() -> attempt(() -> {
            IMovableEntity ent = getMovableEntity(provider.readVarInt());

            if (ent != null) {
                ent.readPosition(provider);
            }
        }));
    }

    public IMovableEntity getMovableEntity(int entId) {
        IMovableEntity ent = players.get(entId);
        if (ent != null) {
            return ent;
        }

        return entities.get(entId);
    }

    public List<SpecificTag> getEntitiesNbt(CoordinateDim2D location) {
        Set<Entity> entities = perChunk.get(location);

        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream().map(Entity::toNbt).collect(Collectors.toList());
    }

    public void reset() {
        this.entities.clear();
        this.perChunk.clear();
        this.players.clear();
    }

    public void addEquipment(DataTypeProvider provider) {
        this.executor.execute(() -> attempt(() -> {
            int id = provider.readVarInt();
            Entity ent = entities.get(id);

            if (ent != null) {
                ent.addEquipment(provider);
            }
        }));
    }

    /**
     * When destroyEntities is called, we don't remove the entities from the perChunk map. These will only be removed
     * when the chunk is unloaded. This way we won't accidentally delete entities that belong to an unsaved chunk.
     */
    public void destroyEntities(DataTypeProvider provider) {
        int count = provider.readVarInt();
        while (count-- > 0) {
            int id = provider.readVarInt();
            if (entities.containsKey(id)) {
                entities.remove(id);
            } else {
                players.remove(id);
            }
        }
    }

    public int countActiveEntities() {
        return this.entities.size();
    }
    public int countActivePlayers() {
        return this.players.size();
    }

    public Collection<PlayerEntity> getPlayerSet() {
        return players.values();
    }

    public void addVillagerTrades(DataTypeProvider provider) {
        this.executor.execute(() -> attempt(() -> {
            worldManager.getVillagerManager().parseAndStoreVillagerTrade(provider);
        }));
    }
}
