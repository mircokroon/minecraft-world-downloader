package game.data.chunk.version;

import config.Config;
import config.Version;
import game.data.chunk.ChunkSection;
import game.data.chunk.palette.Palette;
import game.data.coordinates.CoordinateDim2D;
import game.protocol.Protocol;
import java.util.BitSet;
import java.util.InputMismatchException;
import java.util.function.Function;
import javafx.util.Pair;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import se.llbit.nbt.SpecificTag;

public class Chunk_1_17 extends Chunk_1_16 {
    static int minSectionY = -1;
    static int minBlockSectionY = 0;
    static int maxBlockSectionY = 15;
    static int fullHeight;

    public Chunk_1_17(CoordinateDim2D location, int version) {
        super(location, version);
    }

    public static void setWorldHeight(int min_y, int height) {
        fullHeight = height;
        minBlockSectionY = min_y >> 4;
        minSectionY = minBlockSectionY - 1;
        maxBlockSectionY = minBlockSectionY + (height >> 4) - 1;
    }

    /**
     * Parse the chunk data.
     *
     * @param dataProvider network input
     */
    @Override
    protected void parse(DataTypeProvider dataProvider) {
        raiseEvent("parse from packet");

        BitSet mask = dataProvider.readBitSet();

        parseHeightMaps(dataProvider);

        int biomeSize = dataProvider.readVarInt();
        setBiomes(dataProvider.readVarIntArray(biomeSize));

        int size = dataProvider.readVarInt();
        readChunkColumn(true, mask, dataProvider.ofLength(size));

        parseBlockEntities(dataProvider);
        afterParse();
    }

    /**
     * Generate network packet for this chunk.
     */
    public PacketBuilder toPacket() {
        Protocol p = Config.versionReporter().getProtocol();
        PacketBuilder packet = new PacketBuilder();
        packet.writeVarInt(p.clientBound("LevelChunk"));

        packet.writeInt(location.getX());
        packet.writeInt(location.getZ());
        writeBitSet(packet);
        writeHeightMaps(packet);
        writeBiomes(packet);
        writeChunkSections(packet);

        // we don't include block entities - these chunks will be far away so they shouldn't be rendered anyway
        packet.writeVarInt(0);
        return packet;
    }

    protected PacketBuilder writeSectionData() {
        PacketBuilder column = new PacketBuilder();
        for (ChunkSection section : getAllSections()) {
            if (section.getY() >= getMinBlockSection()) {
                section.write(column);
            }
        }

        return column;
    }

    private void writeBitSet(PacketBuilder packet) {
        BitSet res = new BitSet();
        for (ChunkSection section : getAllSections()) {
            if (section.getY() >= getMinBlockSection()) {
                res.set(section.getY() - getMinBlockSection());
            }
        }

        packet.writeBitSet(res);
    }

    @Override
    public PacketBuilder toLightPacket() {
        PacketBuilder packet = buildLightPacket();

        Pair<BitSet, PacketBuilder> skyLight = writeLightToPacket(ChunkSection::getSkyLight);
        Pair<BitSet, PacketBuilder> blockLight = writeLightToPacket(ChunkSection::getBlockLight);

        packet.writeBitSet(skyLight.getKey());
        packet.writeBitSet(blockLight.getKey());

        // empty masks we just set to 0
        packet.writeBitSet(new BitSet());
        packet.writeBitSet(new BitSet());

        packet.writeVarInt(skyLight.getKey().cardinality());
        packet.writeByteArray(skyLight.getValue().toArray());

        packet.writeVarInt(blockLight.getKey().cardinality());
        packet.writeByteArray(blockLight.getValue().toArray());

        return packet;
    }


    @Override
    public void updateLight(DataTypeProvider provider) {
        boolean isTrusted = provider.readBoolean();

        BitSet skyLightMask = provider.readBitSet();
        BitSet blockLightMask = provider.readBitSet();

        BitSet emptySkyLightMask = provider.readBitSet();
        BitSet emptyBlockLightMask = provider.readBitSet();

        int numSkyLight = provider.readVarInt();
        if (skyLightMask.cardinality() != numSkyLight) {
            throw new InputMismatchException("Number of provided skylight maps does not match provided mask: " + skyLightMask + " != " + numSkyLight);
        }

        parseLightArray(skyLightMask, emptySkyLightMask, provider, ChunkSection::setSkyLight, ChunkSection::getSkyLight);

        int numBlockLight = provider.readVarInt();
        if (blockLightMask.cardinality() != numBlockLight) {
            throw new InputMismatchException("Number of provided blocklight maps does not match provided mask: " + blockLightMask + " != " + numBlockLight);
        }
        parseLightArray(blockLightMask, emptyBlockLightMask, provider, ChunkSection::setBlockLight, ChunkSection::getBlockLight);
    }


    /**
     * Write one of the light arrays to a packet, return the mask and the array itself.
     */
    private Pair<BitSet, PacketBuilder> writeLightToPacket(Function<ChunkSection, byte[]> fn) {
        PacketBuilder packet = new PacketBuilder();
        BitSet mask = new BitSet();

        for (ChunkSection section : getAllSections()) {
            byte[] light = fn.apply(section);
            if (light == null || light.length == 0) { continue; }

            packet.writeVarInt(light.length);
            packet.writeByteArray(light);

            mask.set(section.getY() - getMinSection());
        }


        return new Pair<>(mask, packet);
    }

    @Override
    protected int getMinSection() {
        return minSectionY;
    }

    @Override
    protected int getMinBlockSection() {
        return minBlockSectionY;
    }

    @Override
    protected int getMaxSection() {
        return maxBlockSectionY;
    }

    @Override
    public boolean hasSeparateEntities() {
        return true;
    }
}
