package game.data.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.palette.BlockState;
import game.data.commandblock.CommandBlock;
import game.data.container.InventoryWindow;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.dimension.Dimension;
import game.data.registries.RegistryManager;
import packets.builder.Chat;
import packets.builder.MessageTarget;
import packets.builder.PacketBuilder;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntArrayTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.SpecificTag;
import se.llbit.nbt.StringTag;
import se.llbit.nbt.Tag;

/**
 * Manage entities and block entities for chunks.
 */
public abstract class ChunkEntities extends ChunkEvents {
    // Block-entity NBT fields that hold contents worth keeping across chunk re-sends / block updates
    // (container items, lectern book, jukebox record, custom name, loot table, etc.).
    private static final String[] PRESERVED_CONTENT_FIELDS = {
        "Items", "Item", "Book", "RecordItem", "CustomName", "Lock", "LootTable", "LootTableSeed"
    };

    private final Map<Coordinate3D, SpecificTag> blockEntities;

    public ChunkEntities() {
        super();

        blockEntities = new HashMap<>();
    }

    /**
     * Global positions of all known block entities in this chunk. Returns a snapshot copy so callers
     * on other threads (e.g. the auto-opener) can iterate without risking a ConcurrentModification.
     */
    public java.util.List<Coordinate3D> getBlockEntityPositions() {
        return new java.util.ArrayList<>(blockEntities.keySet());
    }

    /**
     * Whether the block entity at the given global position already has a non-empty Items list
     * (so the auto-opener can skip containers whose contents were already captured).
     */
    public boolean hasCapturedItems(Coordinate3D global) {
        SpecificTag tag = blockEntities.get(global);
        if (!(tag instanceof CompoundTag)) {
            return false;
        }
        Tag items = ((CompoundTag) tag).get("Items");
        return items instanceof ListTag && ((ListTag) items).size() > 0;
    }

    /**
     * Add inventory items to a block entity (e.g. a chest)
     */
    public void addInventory(InventoryWindow window, boolean sendMessages) {
        CompoundTag blockEntity = (CompoundTag) blockEntities.get(window.getContainerLocation());

        // if a block entity is missing, try to generate it first. If there's no block there we don't save anything.
        if (blockEntity == null) {
            BlockState bs = getBlockStateAt(window.getContainerLocation().withinChunk());
            if (bs == null) {
                if (sendMessages) {
                    sendInventoryFailureMessage(window);
                }
                return;
            }
            blockEntity = generateBlockEntity(bs.getName(),  window.getContainerLocation());
            blockEntities.put(window.getContainerLocation(), blockEntity);
        }

        String type = RegistryManager.getInstance().getMenuRegistry().getName(window.getType());

        if (type.equals("minecraft:lectern")) {
            blockEntity.add("Book", window.getSlotsNbt().get(0).asCompound());
        } else {
            blockEntity.add("Items", new ListTag(Tag.TAG_COMPOUND, window.getSlotsNbt()));
        }

        if (window.hasCustomName()) {
            blockEntity.add("CustomName", new StringTag(window.getWindowTitle()));
        }

        WorldManager.getInstance().touchChunk(this);
        
        if (sendMessages) {
            sendInventoryMessage(window);
        }
    }

    private void sendInventoryFailureMessage(InventoryWindow window) {
        if (Config.sendInfoMessages()) {
            Chat message = new Chat("Unable to save inventory at " + window.getContainerLocation() + ". Try reloading the chunk.");
            message.setColor("red");

            Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }

    private void sendInventoryMessage(InventoryWindow blockEntity) {
        if (Config.sendInfoMessages()) {
            String message = "Hui Downloader saved inventory at " + blockEntity.getContainerLocation();
            Config.getPacketInjector().enqueuePacket(PacketBuilder.constructClientMessage(message, MessageTarget.GAMEINFO));
        }
    }

    /**
     * Add command block data to a block entity (a command block)
     */
    public void addCommandBlock(CommandBlock commandBlock) {
        CompoundTag blockEntity = (CompoundTag) blockEntities.get(commandBlock.getLocation());

        // if a block entity is missing, try to generate it first. If there's no block there we don't save anything.
        if (blockEntity == null) {
            BlockState state = getBlockStateAt(commandBlock.getLocation().withinChunk());
            if (state == null) {
//                sendInventoryFailureMessage(window);
                return;
            }
            blockEntity = generateBlockEntity(state.getName(), commandBlock.getLocation());
            blockEntities.put(commandBlock.getLocation(), blockEntity);
        }
        
        commandBlock.addNbt(blockEntity);
        WorldManager.getInstance().touchChunk(this);
    }

    /**
     * Before 1.18
     */
    protected void addLevelNbtTags(CompoundTag map) {
        map.add("TileEntities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(blockEntities.values())));

        if (!hasSeparateEntities()) {
            map.add("Entities", getEntitiesNbt());
        }
    }

    /**
     * For 1.18+
     */
    protected void addBlockEntities(CompoundTag map) {
        map.add("block_entities", new ListTag(Tag.TAG_COMPOUND, new ArrayList<>(blockEntities.values())));
    }


    private ListTag getEntitiesNbt() {
        return new ListTag(Tag.TAG_COMPOUND, WorldManager.getInstance().getEntityRegistry().getEntitiesNbt(this.getLocation()));
    }


    public void addBlockEntity(Coordinate3D location, SpecificTag tag) {
        // we shouldn't reach this state, but just in case we do
        if (tag.tagType() == Tag.TAG_END) {
            return;
        }
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


            // some servers send slightly incorrect block entity IDs (e.g. chest for trapped_chests), we can fix those
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

        // Preserve container contents we already recorded. A chunk re-send / block update usually carries
        // an EMPTY container (servers don't expose contents in chunk data), so without this an inventory
        // we saved earlier would be overwritten and effectively "unsaved" when the chunk is revisited.
        // Only fields the incoming entity does not already provide are carried over, so genuine updates win.
        SpecificTag previous = blockEntities.get(location);
        if (previous instanceof CompoundTag prev) {
            for (String field : PRESERVED_CONTENT_FIELDS) {
                if (entity.get(field).isError() && !prev.get(field).isError()) {
                    entity.add(field, (SpecificTag) prev.get(field));
                }
            }
        }

        blockEntities.put(location, tag);
        WorldManager.getInstance().touchChunk(this);

        // check for inventory contents we previously saved
        CoordinateDim3D pos = location.addDimension3D(getDimension());
        WorldManager.getInstance().getContainerManager().loadPreviousInventoriesAt(this, pos);
        WorldManager.getInstance().getCommandBlockManager().loadPreviousCommandBlockAt(this, pos);
    }

    /**
     * Pre-populate block entities (and their saved contents) from a copy of this chunk previously written
     * to disk. When a chunk is revisited — typically in a later session — the server re-sends it with empty
     * containers; seeding the saved contents first means the live parse (which runs through addBlockEntity,
     * preserving contents) won't "unsave" inventories we already recorded to disk. Only content-bearing
     * entities are seeded, to avoid leaving phantom block entities behind.
     */
    public void seedBlockEntitiesFromDisk(Tag root) {
        if (root == null) { return; }

        Tag list = root.get("block_entities");
        if (list.isError()) {
            Tag level = root.get("Level");
            if (!level.isError()) {
                list = level.asCompound().get("TileEntities");
            }
        }
        if (list.isError()) { return; }

        list.asList().forEach(t -> {
            if (!(t instanceof CompoundTag be) || !hasPreservedContent(be)) { return; }

            Tag xt = be.get("x");
            Tag yt = be.get("y");
            Tag zt = be.get("z");
            if (xt.isError() || yt.isError() || zt.isError()) { return; }

            // Saved coordinates are offset by the world centre; undo that to recover the true global
            // coordinate the live parse uses as the block-entity map key.
            Coordinate3D location = new Coordinate3D(
                xt.intValue() + Config.getCenterX(),
                yt.intValue(),
                zt.intValue() + Config.getCenterZ()
            );
            blockEntities.put(location, be);
        });
    }

    private static boolean hasPreservedContent(CompoundTag be) {
        for (String field : PRESERVED_CONTENT_FIELDS) {
            if (!be.get(field).isError()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a block entity, if the position is not known yet. This method will also offset the position.
     *
     * @param nbtTag the NBT data of the block entity, should include X, Y, Z of the entity
     */
    protected void addBlockEntity(SpecificTag nbtTag) {
        if (!(nbtTag instanceof CompoundTag)) {
            System.out.println("Block entity is not a compound");
            return;
        }

        CompoundTag entity = (CompoundTag) nbtTag;
        Coordinate3D position = new Coordinate3D(entity.get("x").intValue(), entity.get("y").intValue(), entity.get("z").intValue());

        addBlockEntity(position, nbtTag);
    }

    protected CompoundTag generateBlockEntity(String id, Coordinate3D containerLocation) {
        String entId = id;

        // TODO: make a list of these
        // all shulker colours have the same block entity
        if (id.endsWith("shulker_box")) {
            entId = "minecraft:shulker_box";
        }
        // Covers all bed colours
        if (id.endsWith("_bed")) {
            entId = "minecraft:bed";
        }
        // Covers command blocks, chain and repeating command blocks
        if (id.endsWith("command_block")) {
            entId = "minecraft:command_block";
        }
        // Covers banners
        if (id.endsWith("banner")) {
            entId = "minecraft:banner";
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
     * For 1.17+, entities are stored separately from blocks and block entities. This method constructs the NBT object
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
