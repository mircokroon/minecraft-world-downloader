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
    private final Map<Coordinate3D, SpecificTag> blockEntities;

    public ChunkEntities() {
        super();

        blockEntities = new HashMap<>();
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
            String message = "Recorded inventory at " + blockEntity.getContainerLocation();
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

        blockEntities.put(location, tag);
        WorldManager.getInstance().touchChunk(this);

        // check for inventory contents we previously saved
        CoordinateDim3D pos = location.addDimension3D(getDimension());
        WorldManager.getInstance().getContainerManager().loadPreviousInventoriesAt(this, pos);
        WorldManager.getInstance().getCommandBlockManager().loadPreviousCommandBlockAt(this, pos);
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
