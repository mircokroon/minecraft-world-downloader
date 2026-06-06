package game.data.chunk.version;

import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.LongArrayTag;

/**
 * 26.1 (protocol 775) chunk handling.
 *
 * The only change relevant to chunk parsing since 1.20 is the height-map encoding in the chunk-data
 * packet. Up to 1.21.4 the height maps were sent as a single NBT compound; since 1.21.5 they are sent
 * as a length-prefixed array of {@code (type, packed-long-array)} entries. We translate that wire form
 * into the NBT compound Minecraft uses in region files - keyed by the same names - so that both saving
 * chunks to disk and re-sending them to the client to extend the render distance keep working unchanged.
 *
 * Everything else in the chunk-data packet (paletted block/biome sections, block entities, the light
 * masks and arrays) is identical to 1.18+, so this class only overrides the height-map handling.
 */
public class Chunk_26_1 extends Chunk_1_20 {
    /**
     * Network height-map type id -> region-file NBT key. The two world-gen-only maps
     * (0 = world_surface_wg, 2 = ocean_floor_wg) are not stored in region files, so they map to
     * {@code null} and are dropped on parse / skipped on write.
     */
    private static final String[] HEIGHTMAP_KEYS = {
            null,                          // 0: world_surface_wg (world-gen only)
            "WORLD_SURFACE",               // 1
            null,                          // 2: ocean_floor_wg (world-gen only)
            "OCEAN_FLOOR",                 // 3
            "MOTION_BLOCKING",             // 4
            "MOTION_BLOCKING_NO_LEAVES"    // 5
    };

    public Chunk_26_1(CoordinateDim2D location, int version) {
        super(location, version);
    }

    /**
     * Read the height maps from the network as a length-prefixed array of (type, long[]) entries and
     * store them as the region-file NBT compound.
     */
    @Override
    protected void parseHeightMaps(DataTypeProvider dataProvider) {
        CompoundTag compound = new CompoundTag();

        int count = dataProvider.readVarInt();
        for (int i = 0; i < count; i++) {
            int type = dataProvider.readVarInt();
            long[] data = dataProvider.readLongArray(dataProvider.readVarInt());

            if (type >= 0 && type < HEIGHTMAP_KEYS.length && HEIGHTMAP_KEYS[type] != null) {
                compound.add(HEIGHTMAP_KEYS[type], new LongArrayTag(data));
            }
        }

        heightMap = compound;
    }

    /**
     * Re-encode the stored height maps into the 1.21.5+ array form when streaming a chunk back to the
     * client. Only the maps we actually hold are written; the client recomputes anything missing.
     */
    @Override
    protected void writeHeightMaps(PacketBuilder packet) {
        if (!(heightMap instanceof CompoundTag compound)) {
            packet.writeVarInt(0);
            return;
        }

        int present = 0;
        for (String key : HEIGHTMAP_KEYS) {
            if (key != null && !compound.get(key).isError()) {
                present++;
            }
        }

        packet.writeVarInt(present);
        for (int type = 0; type < HEIGHTMAP_KEYS.length; type++) {
            String key = HEIGHTMAP_KEYS[type];
            if (key == null || compound.get(key).isError()) {
                continue;
            }

            long[] data = compound.get(key).longArray();
            packet.writeVarInt(type);
            packet.writeVarInt(data.length);
            packet.writeLongArray(data);
        }
    }
}
