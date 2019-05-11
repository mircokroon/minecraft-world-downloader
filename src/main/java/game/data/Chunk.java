package game.data;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.ShortTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;

import game.Game;
import packets.DataTypeProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import javax.xml.crypto.Data;

public class Chunk {
    public static HashMap<Coordinate2D, Chunk> existingChunks = new HashMap<>();
    private static final int CHUNK_HEIGHT = 256;
    private static final int SECTION_HEIGHT = 16;
    private static final int SECTION_WIDTH = 16;

    private static final int DataVersion = 1631;

    private int x;
    private int z;
    private List<CompoundTag> tileEntities;
    private ChunkSection[] chunkSections;
    private byte[] biomes;

    public Chunk(int x, int z) {
        this.x = x;
        this.z = z;

        chunkSections = new ChunkSection[16];
        tileEntities = new ArrayList<>();
        this.biomes = new byte[256];

        existingChunks.put(new Coordinate2D(x, z), this);
    }

    public static Chunk readChunkDataPacket(DataTypeProvider dataProvider) {
        int x = dataProvider.readInt();
        int z = dataProvider.readInt();

        boolean full = dataProvider.readBoolean();
        Chunk chunk;
        if (full) {
            chunk = new Chunk(x, z);
        } else {
            chunk = existingChunks.get(new Coordinate2D(x, z));
        }
        int mask = dataProvider.readVarInt();
        int size = dataProvider.readVarInt();
        readChunkColumn(chunk, full, mask, dataProvider);

        int tileEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < tileEntityCount; i++) {
            chunk.addTileEntity(dataProvider.readCompoundTag());
        }

        return chunk;
    }

    private void addTileEntity(CompoundTag tag) {
        tileEntities.add(tag);
    }


    private static int getInt(CompoundTag tag, String name) {
        IntTag intTag = (IntTag) tag.getValue().get(name);
        return intTag.getValue();
    }

    private static String getString(CompoundTag tag, String name) {
        StringTag intTag = (StringTag) tag.getValue().get(name);
        return intTag.getValue();
    }

    private static void readChunkColumn(Chunk chunk, boolean full, int mask, DataTypeProvider dataProvider) {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?
                byte bitsPerBlock = dataProvider.readNext();

                Palette palette = Palette.readPalette(bitsPerBlock, dataProvider);

                // A bitmask that contains bitsPerBlock set bits
                int individualValueMask = (1 << bitsPerBlock) - 1;

                int dataArrayLength = dataProvider.readVarInt();
                long[] dataArray = dataProvider.readLongArray(dataArrayLength);

                ChunkSection section = new ChunkSection(sectionY);

                for (int y = 0; y < SECTION_HEIGHT; y++) {
                    for (int z = 0; z < SECTION_WIDTH; z++) {
                        for (int x = 0; x < SECTION_WIDTH; x++) {
                            int blockNumber = (((y * SECTION_HEIGHT) + z) * SECTION_WIDTH) + x;
                            int startLong = (blockNumber * bitsPerBlock) / 64;
                            int startOffset = (blockNumber * bitsPerBlock) % 64;
                            int endLong = ((blockNumber + 1) * bitsPerBlock - 1) / 64;

                            int data;
                            if (startLong == endLong) {
                                data = (int) (dataArray[startLong] >>> startOffset);
                            } else {
                                int endOffset = 64 - startOffset;
                                data = (int) (dataArray[startLong] >>> startOffset | dataArray[endLong] << endOffset);
                            }
                            data &= individualValueMask;

                            // data should always be valid for the palette
                            // If you're reading a power of 2 minus one (15, 31, 63, 127, etc...) that's out of bounds,
                            // you're probably reading light data instead

                            BlockState state = palette.StateForId(data);
                            section.setState(x, y, z, state);
                        }
                    }
                }


                for (int y = 0; y < SECTION_HEIGHT; y++) {
                    for (int z = 0; z < SECTION_WIDTH; z++) {
                        for (int x = 0; x < SECTION_WIDTH; x += 2) {
                            // Note: x += 2 above; we read 2 values along x each time
                            byte value = dataProvider.readNext();

                            section.setBlockLight(x, y, z, (byte) (value & 0xF));
                            section.setBlockLight(x + 1, y, z, (byte)((value >> 4) & 0xF));
                        }
                    }
                }

                if (Game.getDimension() == Dimension.OVERWORLD) {
                    for (int y = 0; y < SECTION_HEIGHT; y++) {
                        for (int z = 0; z < SECTION_WIDTH; z++) {
                            for (int x = 0; x < SECTION_WIDTH; x += 2) {
                                // Note: x += 2 above; we read 2 values along x each time
                                byte value = dataProvider.readNext();

                                section.setSkyLight(x, y, z, (byte) (value & 0xF));
                                section.setSkyLight(x + 1, y, z, (byte) ((value >> 4) & 0xF));
                            }
                        }
                    }
                }

                // May replace an existing section or a null one
                chunk.setSection(sectionY, section);
            }
        }

        if (full) {
            for (int z = 0; z < SECTION_WIDTH; z++) {
                for (int x = 0; x < SECTION_WIDTH; x++) {
                    chunk.setBiome(x, z, dataProvider.readNext());
                }
            }
        }
    }

    private void setBiome(int x, int z, byte biomeId) {
        biomes[x * 16 + z] = biomeId;
    }

    private void setSection(int sectionY, ChunkSection section) {
        chunkSections[sectionY] = section;
    }

    public static Chunk getChunk(Coordinate3D coordinate) {
        return existingChunks.get(coordinate.chunkPos());
    }

    public CompoundTag toNbt() {
        CompoundMap rootMap = new CompoundMap();
        rootMap.put("Level", createNbtLevel());
        rootMap.put("DataVersion", new IntTag("DataVersion", DataVersion));

        return new CompoundTag("", rootMap);
    }

    private CompoundTag createNbtLevel() {
        CompoundMap levelMap = new CompoundMap();
        levelMap.put(new IntTag("xPos", x));
        levelMap.put(new IntTag("zPos", z));
        levelMap.put(new ByteTag("TerrainPopulated", (byte) 1));
        levelMap.put(new ByteTag("LightPopulated", (byte) 1));
        levelMap.put(new LongTag("InhabitedTime", 0));
        levelMap.put(new LongTag("LastUpdate", 0));
        levelMap.put(new ListTag<>("Entities", CompoundTag.class, new ArrayList<>()));

        levelMap.put(new ByteArrayTag("Biomes", biomes));
        levelMap.put(new ListTag<>("TileEntities", CompoundTag.class, tileEntities));
        levelMap.put(new ListTag<>("Sections", CompoundTag.class, getSectionList()));

        return new CompoundTag("Level", levelMap);
    }

    private List<CompoundTag> getSectionList() {
        return Arrays.stream(chunkSections)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(ChunkSection::getY))
            .map(ChunkSection::toNbt)
            .collect(Collectors.toList());
    }
}
