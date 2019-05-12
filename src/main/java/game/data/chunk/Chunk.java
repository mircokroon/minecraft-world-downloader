package game.data.chunk;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.StringTag;

import game.Game;
import game.data.Coordinate2D;
import game.data.Dimension;
import game.data.WorldManager;
import packets.DataTypeProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Chunk {
    private static final int LIGHT_SIZE = 2048;
    private static final int CHUNK_HEIGHT = 256;
    private static final int SECTION_HEIGHT = 16;
    private static final int SECTION_WIDTH = 16;

    private static final int DataVersion = 1631;

    private int x;
    private int z;
    private List<CompoundTag> tileEntities;
    private ChunkSection[] chunkSections;
    private byte[] biomes;

    private boolean saved;

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public Chunk(int x, int z) {
        this.saved = false;
        this.x = x;
        this.z = z;

        chunkSections = new ChunkSection[16];
        tileEntities = new ArrayList<>();
        this.biomes = new byte[256];

        WorldManager.addChunk(new Coordinate2D(x, z), this);
    }

    public void addTileEntity(CompoundTag tag) {
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

    public void readChunkColumn(boolean full, int mask, DataTypeProvider dataProvider) {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?
                byte bitsPerBlock = dataProvider.readNext();

                Palette palette = Palette.readPalette(bitsPerBlock, dataProvider);

                // A bitmask that contains bitsPerBlock set bits
                int individualValueMask = (1 << bitsPerBlock) - 1;

                int dataArrayLength = dataProvider.readVarInt();
                long[] dataArray = dataProvider.readLongArray(dataArrayLength);

                ChunkSection section = new ChunkSection(sectionY);

                if (dataArrayLength == 0) {
                    return;
                }

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

                section.setBlockLight(dataProvider.readByteArray(LIGHT_SIZE));

                if (Game.getDimension() == Dimension.OVERWORLD) {
                    section.setSkyLight(dataProvider.readByteArray(LIGHT_SIZE));
                }

                // May replace an existing section or a null one
                setSection(sectionY, section);
            }
        }

        if (full) {
            for (int z = 0; z < SECTION_WIDTH; z++) {
                for (int x = 0; x < SECTION_WIDTH; x++) {
                    setBiome(x, z, dataProvider.readNext());
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
