package game.data;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;

public class ChunkSection {
    BlockState[][][] blocks;
    byte[] blockLight;
    byte[] skyLight;
    int y;

    public ChunkSection(int y) {
        this.y = y;
        this.blocks = new BlockState[16][16][16];
        this.blockLight = new byte[2048];
        this.skyLight = new byte[2048];
    }

    public void setState(int x, int y, int z, BlockState state) {
        this.blocks[x][y][z] = state;
    }

    public void setBlockLight(int x, int y, int z, byte lightValue) {
        insertAtHalf(blockLight, x, y, z, lightValue);
    }

    public void setSkyLight(int x, int y, int z, byte lightValue) {
        insertAtHalf(skyLight, x, y, z, lightValue);
    }

    public CompoundTag toNbt() {
        CompoundMap map = new CompoundMap();
        map.put(new IntTag("Y", y));

        map.put(new ByteArrayTag("Blocks", getBlockIds()));
        map.put(new ByteArrayTag("Data", getBlockStates()));
        map.put(new ByteArrayTag("SkyLight", skyLight));
        map.put(new ByteArrayTag("BlockLight", blockLight));

        return new CompoundTag("", map);
    }

    public byte[] getBlockIds() {
        byte[] blockData = new byte[4096];

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    blockData[getBlockIndex(x, y, z)] = (byte) blocks[x][y][z].id;
                }
            }

        }
        return blockData;
    }

    public byte[] getBlockStates() {
        byte[] blockData = new byte[2048];

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    insertAtHalf(blockData, x, y, z, blocks[x][y][z].meta);
                }
            }

        }
        return blockData;
    }

    private static void insertAtHalf(byte[] arr, int x, int y, int z, int val) {
        int pos = getBlockIndex(x, y, z);
        boolean isUpperHalf = pos % 2 == 0;
        if (isUpperHalf) {
            arr[pos / 2] |= val << 4;
        } else {
            arr[pos / 2] |= val & 0x0F;
        }
    }


    public static int getBlockIndex(int x, int y, int z) {
        return y*16*16 + z*16 + x;
    }

    public int getY() {
        return y;
    }
}
