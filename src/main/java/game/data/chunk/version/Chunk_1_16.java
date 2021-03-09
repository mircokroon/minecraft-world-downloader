package game.data.chunk.version;

import config.Config;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.protocol.Protocol;
import javafx.util.Pair;
import packets.DataTypeProvider;
import packets.builder.DebugPacketBuilder;
import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Support for chunks of version 1.16.2+. 1.16.0 and 1.16.1 are not supported.
 */
public class Chunk_1_16 extends Chunk_1_15 {
    public static final int DATA_VERSION = 2578;

    public Chunk_1_16(CoordinateDim2D location) {
        super(location);
    }

    @Override
    public int getDataVersion() { return DATA_VERSION; }


    // 1.16.2 changes biomes from int[1024] to varint[given length]
    @Override
    protected void parse3DBiomeData(DataTypeProvider provider) {
        int biomesLength = provider.readVarInt();
        setBiomes(provider.readVarIntArray(biomesLength));
    }

    @Override
    protected ChunkSection createNewChunkSection(byte y, Palette palette) {
        return new ChunkSection_1_16(y, palette);
    }

    @Override
    protected ChunkSection parseSection(int sectionY, SpecificTag section) {
        return new ChunkSection_1_16(sectionY, section);
    }

    @Override
    protected void writeBiomes(PacketBuilder packet) {
        int[] biomes = getBiomes();
        packet.writeVarInt(biomes.length);
        packet.writeVarIntArray(biomes);
    }

    @Override
    public void updateBlocks(Coordinate3D pos, DataTypeProvider provider) {
        provider.readBoolean();

        int count = provider.readVarInt();
        Collection<Coordinate3D> toUpdate = new ArrayList<>();
        while (count-- > 0) {
            long blockChange = provider.readVarLong();
            int blockId = (int) blockChange >>> 12;

            int x = (int) (blockChange >> 8) & 0x0F;
            int z = (int) (blockChange >> 4) & 0x0F;
            int y = (int) (blockChange     ) & 0x0F;

            // since updateBlock expects the height to be [0-256], we add in the section coordinates.
            Coordinate3D blockPos = new Coordinate3D(x, pos.getY() * 16 + y, z);
            toUpdate.add(blockPos);

            updateBlock(blockPos, blockId, true);
        }
        this.getChunkImageFactory().recomputeHeights(toUpdate);
    }

    @Override
    public void updateLight(DataTypeProvider provider) {
        super.updateLight(provider);

        boolean isTrusted = provider.readBoolean();

        int skyLightMask = provider.readVarInt();
        int blockLightMask = provider.readVarInt();

        int emptySkyLightMask = provider.readVarInt();
        int emptyBlockLightMask = provider.readVarInt();


        parseLightArray(skyLightMask, emptySkyLightMask, provider, ChunkSection::setSkyLight);
        parseLightArray(blockLightMask, emptyBlockLightMask, provider, ChunkSection::setBlockLight);
    }

    private void parseLightArray(int mask, int emptyMask, DataTypeProvider provider, BiConsumer<ChunkSection, byte[]> c) {
        for (int sectionY = -1; (mask != 0 || emptyMask != 0); sectionY++, mask >>>= 1, emptyMask >>>= 1) {

            ChunkSection s = null;
            if (sectionY >= 0 && sectionY < getChunkSections().length) {
                s = getChunkSections()[sectionY];;

                // if we get light for empty sections, create the section
                if (s == null) {
                    s = createNewChunkSection((byte) sectionY, Palette.empty());
                    s.setBlocks(new long[256]);

                    getChunkSections()[sectionY] = s;
                }
            }


            // Mask tells us if a section is present or not
            if ((mask & 1) == 0) {
                if (s != null && (emptyMask & 1) != 0) {
                    c.accept(s, new byte[2048]);
                }
                continue;
            }

            int skyLength = provider.readVarInt();
            byte[] data = provider.readByteArray(skyLength);

            if (s != null) {
                c.accept(s, data);
            }

        }
    }

    /**
     * Build the start of a light packet, which is always the same.
     */
    private PacketBuilder buildLightPacket() {
        Protocol p = Config.versionReporter().getProtocol();
        PacketBuilder packet = new PacketBuilder();
        packet.writeVarInt(p.clientBound("chunk_update_light"));

        packet.writeVarInt(location.getX());
        packet.writeVarInt(location.getZ());
        packet.writeBoolean(true);

        return packet;
    }

    @Override
    public PacketBuilder toLightPacket() {
        PacketBuilder packet = buildLightPacket();

        Pair<Integer, PacketBuilder> skyLight = writeLightToPacket(ChunkSection::getSkyLight);
        Pair<Integer, PacketBuilder> blockLight = writeLightToPacket(ChunkSection::getBlockLight);

        packet.writeVarInt(skyLight.getKey());
        packet.writeVarInt(blockLight.getKey());

        // empty masks we just set to 0
        packet.writeVarInt(0);
        packet.writeVarInt(0);

        packet.writeByteArray(skyLight.getValue().toArray());
        packet.writeByteArray(blockLight.getValue().toArray());

        return packet;
    }

    /**
     * Write one of the light arrays to a packet, return the mask and the array itself.
     */
    private Pair<Integer, PacketBuilder> writeLightToPacket(Function<ChunkSection, byte[]> fn) {
        PacketBuilder packet = new PacketBuilder();
        int mask = 0;
        ChunkSection[] sections = getChunkSections();
        for (int sectionY = 0; sectionY < sections.length; sectionY++) {
            if (sections[sectionY] == null) { continue; }

            byte[] light = fn.apply(sections[sectionY]);
            if (light == null || light.length == 0) { continue; }

            packet.writeVarInt(light.length);
            packet.writeByteArray(light);
            mask |= 1 << sectionY + 1;
        }
        return new Pair<>(mask, packet);
    }
}
