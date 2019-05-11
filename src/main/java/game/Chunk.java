package game;

import packets.DataTypeProvider;

import java.util.HashMap;

public class Chunk {
    private static final int CHUNK_HEIGHT = 256;
    private static final int SECTION_HEIGHT = 16;
    private static final int SECTION_WIDTH = 16;
    static HashMap<Coordinate2D, Chunk> existingChunks = new HashMap<>();

    int x;
    int z;
    ChunkSection[] chunkSections = new ChunkSection[16];

    int[][] biomes = new int[16][16];

    public Chunk(int x, int z) {
        this.x = x;
        this.z = z;

        existingChunks.put(new Coordinate2D(x, z), this);
    }

    public static Chunk readChunkDataPacket(DataTypeProvider dataProvider) {
        int x = dataProvider.readInt();
        int z = dataProvider.readInt();
        System.out.println("Parsing chunk at " + x + ", " + z);
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

        /*
        // TODO: parse block entities
        int blockEntityCount = dataProvider.readVarInt();
        for (int i = 0; i < blockEntityCount; i++) {
            CompoundTag tag = ReadCompoundTag(data);
            chunk.AddBlockEntity(tag.GetInt("x"), tag.GetInt("y"), tag.GetInt("z"), tag);
        }
        */
        return chunk;
    }

    private static void readChunkColumn(Chunk chunk, boolean full, int mask, DataTypeProvider dataProvider) {
        for (int sectionY = 0; sectionY < (CHUNK_HEIGHT / SECTION_HEIGHT); sectionY++) {
            if ((mask & (1 << sectionY)) != 0) {  // Is the given bit set in the mask?
                byte bitsPerBlock = dataProvider.readNext();
                System.out.println("Bits per block: " + bitsPerBlock);

                if (bitsPerBlock > 4) {
                    System.out.println("Too many bits per block!");
                    return;
                }

                Palette palette = Palette.readPalette(bitsPerBlock, dataProvider);

                // A bitmask that contains bitsPerBlock set bits
                int individualValueMask = (1 << bitsPerBlock) - 1;

                int dataArrayLength = dataProvider.readVarInt();
                long[] dataArray = dataProvider.readLongArray(dataArrayLength);

                ChunkSection section = new ChunkSection();

                for (int y = 0; y < SECTION_HEIGHT; y++) {
                    for (int z = 0; z < SECTION_WIDTH; z++) {
                        for (int x = 0; x < SECTION_WIDTH; x++) {
                            int blockNumber = (((y * SECTION_HEIGHT) + z) * SECTION_WIDTH) + x;
                            int startLong = (blockNumber * bitsPerBlock) / 64;
                            int startOffset = (blockNumber * bitsPerBlock) % 64;
                            int endLong = ((blockNumber + 1) * bitsPerBlock - 1) / 64;

                            int data;
                            if (startLong == endLong) {
                                data = (int) (dataArray[startLong] >> startOffset);
                            } else {
                                int endOffset = 64 - startOffset;
                                data = (int) (dataArray[startLong] >> startOffset | dataArray[endLong] << endOffset);
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

                            section.setBlockLight(x, y, z, value & 0xF);
                            section.setBlockLight(x + 1, y, z, (value >> 4) & 0xF);
                        }
                    }
                }

                if (Game.getDimension() == Dimension.OVERWORLD) {
                    for (int y = 0; y < SECTION_HEIGHT; y++) {
                        for (int z = 0; z < SECTION_WIDTH; z++) {
                            for (int x = 0; x < SECTION_WIDTH; x += 2) {
                                // Note: x += 2 above; we read 2 values along x each time
                                byte value = dataProvider.readNext();

                                section.setSkyLight(x, y, z, value & 0xF);
                                section.setSkyLight(x + 1, y, z, (value >> 4) & 0xF);
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

    private void setBiome(int x, int z, int biomeId) {
        biomes[x][z] = biomeId;
    }

    private void setSection(int sectionY, ChunkSection section) {
        chunkSections[sectionY] = section;
    }

    public static void printBlockInfo(Coordinate3D coordinate) {
        int chunkX = (int) Math.floor(coordinate.getX() / 16);
        int chunkZ = (int) Math.floor(coordinate.getZ() / 16);

        existingChunks.get(new Coordinate2D(chunkX, chunkZ)).printBlockInfoOfChunk(coordinate);
    }

    public void printBlockInfoOfChunk(Coordinate3D coordinates) {
        int sectionY = (int) Math.floor(coordinates.getY() / 16);
        ChunkSection section = chunkSections[sectionY];

        System.out.println(section.getBlockInformation(coordinates));
    }
}
