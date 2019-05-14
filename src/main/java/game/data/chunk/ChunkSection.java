package game.data.chunk;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;

/**
 * Class to hold a 16 block tall chunk section.
 */
public class ChunkSection {
    int[][][] blocks;
    byte[] blockLight;
    byte[] skyLight;
    byte y;

    public ChunkSection(byte y) {
        this.y = y;
        this.blocks = new int[16][16][16];
        this.blockLight = new byte[2048];
        this.skyLight = new byte[2048];
    }

    public void setSkyLight(byte[] skyLight) {
        this.skyLight = skyLight;
    }

    public void setBlockLight(byte[] blockLight) {
        this.blockLight = blockLight;
    }

    public void setState(int x, int y, int z, int state) {
        this.blocks[x][y][z] = state;
    }

    /**
     * Convert this section to NBT.
     */
    public CompoundTag toNbt() {
        CompoundMap map = new CompoundMap();
        map.put(new ByteTag("Y", y));

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
                    blockData[getBlockIndex(x, y, z)] = (byte) (blocks[x][y][z] >>> 4);
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
                    insertAtHalf(blockData, x, y, z, blocks[x][y][z] & 0x0F);
                }
            }
        }
        return blockData;
    }

    public static int getBlockIndex(int x, int y, int z) {
        return y * 16 * 16 + z * 16 + x;
    }

    /**
     * Handle inserting of the four-bit values into the given array at the given coordinates. In this case, a value
     * takes up 4 bits so inserting them is a bit more complicated.
     */
    private static void insertAtHalf(byte[] arr, int x, int y, int z, int val) {
        int pos = getBlockIndex(x, y, z);
        boolean isUpperHalf = pos % 2 == 0;
        if (!isUpperHalf) {
            arr[pos / 2] |= (val << 4) & 0xF0;
        } else {
            arr[pos / 2] |= val & 0x0F;
        }
    }

    public byte getY() {
        return y;
    }
}
