package game.data.chunk;

import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.container.InventoryWindow;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.dimension.Dimension;
import se.llbit.nbt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manage entities and tile entities for chunks.
 */
public abstract class ChunkEntities {
    private final Map<Coordinate3D, SpecificTag> tileEntities;

    public ChunkEntities() {
        tileEntities = new HashMap<>();
    }

    /**
     * Add inventory items to a tile entity (e.g. a chest)
     */
    public void addInventory(InventoryWindow window) {
        CompoundTag tileEntity = (CompoundTag) tileEntities.get(window.getContainerLocation());

        // if a tile entity is missing, don't store anything
        if (tileEntity == null) {
            return;
        }

        tileEntity.add("Items", new ListTag(Tag.TAG_COMPOUND, window.getSlotsNbt()));

        if (window.hasCustomName()) {
            tileEntity.add("CustomName", new StringTag(window.getWindowTitle()));
        }

    }

    protected void addLevelNbtTags(CompoundTag map) {
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(tileEntities.values())));

        List<SpecificTag> entities = WorldManager.getInstance().getEntityRegistry().getEntitiesNbt(this.getLocation());
        map.add("Entities", new ListTag(Tag.TAG_COMPOUND, entities));
    }


    public void addTileEntity(Coordinate3D location, SpecificTag tag) {
        CompoundTag entity = (CompoundTag) tag;

        // validate entity identifer
        if (!entity.get("id").isError()) {
            String id = entity.get("id").stringValue();

            if (id.split(":").length < 2) {
                id = "minecraft:" + id.toLowerCase();
            }

            // invalid identifier - some servers will send these and it makes Minecraft angry when we load the world
            if (!id.matches("^[a-z0-9/._-]*$")) {
                entity.add("id", new StringTag(id.toLowerCase()));
            }


            // some servers send slightly incorrect tile entity IDs (e.g. chest for trapped_chests), we can fix those
            // to ensure that the chests still works
            BlockState bs = getBlockStateAt(location.withinChunk());
            if (bs != null) {
                String blockStateName = bs.getName();

                if (!blockStateName.equals(id) && blockStateName.contains("chest")) {
                    entity.add("id", new StringTag(bs.getName()));
                }
            }
        }

        // get offset location
        Coordinate3D offset = location.offsetGlobal();

        // insert new coordinates (offset)
        entity.add("x", new IntTag(offset.getX()));
        entity.add("y", new IntTag(offset.getY()));
        entity.add("z", new IntTag(offset.getZ()));

        tileEntities.put(location, tag);

        // check for inventory contents we previously saved
        CoordinateDim3D pos = location.addDimension3D(getDimension());
        WorldManager.getInstance().getContainerManager().loadPreviousInventoriesAt(this, pos);
    }

    /**
     * Add a tile entity, if the position is not known yet. This method will also offset the position.
     *
     * @param nbtTag the NBT data of the tile entity, should include X, Y, Z of the entity
     */
    protected void addTileEntity(SpecificTag nbtTag) {
        CompoundTag entity = (CompoundTag) nbtTag;
        Coordinate3D position = new Coordinate3D(entity.get("x").intValue(), entity.get("y").intValue(), entity.get("z").intValue());

        addTileEntity(position, nbtTag);
    }

    public abstract Dimension getDimension();
    public abstract CoordinateDim2D getLocation();
    public abstract BlockState getBlockStateAt(Coordinate3D location);
}
