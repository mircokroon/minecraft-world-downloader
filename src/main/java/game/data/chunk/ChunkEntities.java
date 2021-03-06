package game.data.chunk;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.container.InventoryWindow;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.dimension.Dimension;
import packets.builder.Chat;
import packets.builder.MessageTarget;
import packets.builder.PacketBuilder;
import se.llbit.nbt.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage entities and tile entities for chunks.
 */
public abstract class ChunkEntities extends ChunkEvents {
    private final Map<Coordinate3D, SpecificTag> tileEntities;

    public ChunkEntities() {
        super();

        tileEntities = new HashMap<>();
    }

    /**
     * Add inventory items to a tile entity (e.g. a chest)
     */
    public void addInventory(InventoryWindow window) {
        CompoundTag tileEntity = (CompoundTag) tileEntities.get(window.getContainerLocation());

        // if a tile entity is missing, try to generate it first. If there's no block there we don't save anything.
        if (tileEntity == null) {
            BlockState bs = getBlockStateAt(window.getContainerLocation().withinChunk());
            if (bs == null) {
                sendInventoryFailureMessage(window);
                return;
            }
            tileEntity = generateTileEntity(bs.getName(),  window.getContainerLocation());
            tileEntities.put(window.getContainerLocation(), tileEntity);
        }

        tileEntity.add("Items", new ListTag(Tag.TAG_COMPOUND, window.getSlotsNbt()));

        if (window.hasCustomName()) {
            tileEntity.add("CustomName", new StringTag(window.getWindowTitle()));
        }

        WorldManager.getInstance().touchChunk(this);
        sendInventoryMessage(window);
    }

    private void sendInventoryFailureMessage(InventoryWindow window) {
        if (Config.sendInfoMessages()) {
            Chat message = new Chat("Unable to save inventory at " + window.getContainerLocation() + ". Try reloading the chunk.");
            message.setColor("red");

            Config.getPacketInjector().accept(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }

    private void sendInventoryMessage(InventoryWindow tileEntity) {
        if (Config.sendInfoMessages()) {
            String message = "Recorded inventory at " + tileEntity.getContainerLocation();
            Config.getPacketInjector().accept(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }

    protected void addLevelNbtTags(CompoundTag map) {
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(tileEntities.values())));

        if (!hasSeparateEntities()) {
            map.add("Entities", getEntitiesNbt());
        }
    }

    private ListTag getEntitiesNbt() {
        return new ListTag(Tag.TAG_COMPOUND, WorldManager.getInstance().getEntityRegistry().getEntitiesNbt(this.getLocation()));
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

    private CompoundTag generateTileEntity(String id, Coordinate3D containerLocation) {
        String entId = id;

        // all shulker colours have the same tile entity
        if (id.endsWith("shulker_box")) {
            entId =  "minecraft:shulker_box";
        }

        CompoundTag entity = new CompoundTag();
        entity.add("id", new StringTag(entId));
        entity.add("x", new IntTag(containerLocation.getX()));
        entity.add("y", new IntTag(containerLocation.getY()));
        entity.add("z", new IntTag(containerLocation.getZ()));

        return entity;
    }

    public abstract Dimension getDimension();
    public abstract CoordinateDim2D getLocation();
    public abstract BlockState getBlockStateAt(Coordinate3D location);
    public abstract void touch();
    public abstract boolean hasSeparateEntities();
    public abstract int getDataVersion();


    /**
     * For 1.17+, entities are stored separately from blocks and tile entities. This method constructs the NBT object
     * of just the entity file.
     */
    public NamedTag toEntityNbt() {
        if (!hasSeparateEntities()) {
            return null;
        }

        CompoundTag root = new CompoundTag();

        ListTag entities = new ListTag(Tag.TAG_COMPOUND, WorldManager.getInstance().getEntityRegistry().getEntitiesNbt(this.getLocation()));
        if (entities.size() == 0) {
            return null;
        }

        root.add("Entities", entities);
        root.add("DataVersion", new IntTag(getDataVersion()));
        root.add("Position", new IntArrayTag(new int[]{
                getLocation().getX(),
                getLocation().getZ()
        }));

        return new NamedTag("", root);
    }
}
