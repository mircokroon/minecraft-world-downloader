package packets.version;

import game.data.container.Slot;

/**
 * Slot/item reading for 1.20.6+ (including 26.1.x).
 *
 * Since 1.20.5 an item stack is no longer a single NBT tag but a list of "data components". The wire
 * format is:
 *   itemCount: VarInt
 *   if itemCount &gt; 0:
 *     itemId: VarInt
 *     componentsToAdd: VarInt
 *     componentsToRemove: VarInt
 *     add[]:    componentType (VarInt) + type-specific data   (NO length prefix)
 *     remove[]: componentType (VarInt)
 *
 * Components have no length prefix, so to stay aligned every component must be consumed by its exact
 * structure. We consume the common scalar/NBT/string components and the recursive item-list components
 * (shulker container, bundle, crossbow projectiles). Components we don't model yet throw
 * {@link UnsupportedComponentException}; the caller treats that as "this container could not be
 * captured" and moves on. This is always safe: the packet itself is forwarded to the client byte-for-byte
 * regardless of what we parse, so a miss only affects what the downloader saves, never the connection.
 */
public class DataTypeProvider_1_20_6 extends DataTypeProvider_1_20_2 {
    public DataTypeProvider_1_20_6(byte[] finalFullPacket) {
        super(finalFullPacket);
    }

    /** Thrown when an item carries a data component we don't yet know how to read. */
    public static final class UnsupportedComponentException extends RuntimeException {
        public UnsupportedComponentException(int type) {
            super("Unsupported item data component id " + type + " — container/item not saved.");
        }
    }

    @Override
    public Slot readSlot() {
        int count = readVarInt();
        if (count <= 0) {
            return null;
        }

        int itemId = readVarInt();
        int toAdd = readVarInt();
        int toRemove = readVarInt();

        for (int i = 0; i < toAdd; i++) {
            consumeComponent(readVarInt());
        }
        for (int i = 0; i < toRemove; i++) {
            readVarInt(); // removed component type id
        }

        // We don't yet reconstruct component NBT, but the item id + count are preserved.
        return new Slot(itemId, (byte) count, null);
    }

    /**
     * Read (and discard) one data component's payload so the stream stays aligned. Throws for
     * components we don't model yet.
     */
    private void consumeComponent(int type) {
        switch (type) {
            // --- nothing to read (marker components) ---
            case 4:  // unbreakable
            case 20: // creative_slot_lock
            case 22: // intangible_projectile
            case 34: // glider
                return;

            // --- single VarInt ---
            case 1:  // max_stack_size
            case 2:  // max_damage
            case 3:  // damage
            case 12: // rarity
            case 19: // repair_cost
            case 31: // enchantable
            case 44: // map_id
            case 46: // map_post_processing
            case 61: // ominous_bottle_amplifier
            case 71: // base_color
            case 79: case 80: case 81: case 82: case 83: case 84: case 85: // entity variant ids
            case 86: case 87: case 88: case 89: case 90: case 91: case 92:
            case 95: case 96: case 98: case 99: case 100: case 101: case 102: case 103:
                readVarInt();
                return;

            // --- single boolean ---
            case 21: // enchantment_glint_override
                readBoolean();
                return;

            // --- single float ---
            case 7:  // minimum_attack_charge
            case 50: // potion_duration_scale
                readFloat();
                return;

            // --- single int ---
            case 42: // dyed_color
            case 43: // map_color
                readInt();
                return;

            // --- single string ---
            case 10: // item_model
            case 27: // damage_resistant
            case 35: // tooltip_style
            case 63: // provides_banner_patterns
            case 69: // note_block_sound
                readString();
                return;

            // --- single NBT tag (anonymous/nameless) ---
            case 0:  // custom_data
            case 6:  // custom_name
            case 9:  // item_name
            case 45: // map_decorations
            case 55: // debug_stick_state
            case 57: // bucket_entity_data
            case 64: // recipes
            case 76: // lock
            case 77: // container_loot
                readNbtTag();
                return;

            // --- list of NBT tags ---
            case 11: { // lore
                int n = readVarInt();
                for (int i = 0; i < n; i++) readNbtTag();
                return;
            }

            // --- recursive item lists ---
            case 47: // charged_projectiles
            case 48: // bundle_contents
            case 73: { // container (shulker boxes, etc.)
                int n = readVarInt();
                for (int i = 0; i < n; i++) readSlot();
                return;
            }
            case 25: // use_remainder (a single Slot)
                readSlot();
                return;

            default:
                throw new UnsupportedComponentException(type);
        }
    }
}
