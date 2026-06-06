package game.data.chunk;

import config.Config;
import config.Version;
import game.data.WorldManager;
import game.data.chunk.palette.BlockColors;
import game.data.chunk.version.Chunk_1_8;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionRegistry;
import game.data.registries.RegistryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import packets.DataTypeProvider;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validates the 1.8 direct-array chunk parser end to end: build a single-section 1.8 Chunk Data body,
 * parse it through {@link Chunk_1_8}, and check the decoded block states.
 */
class Chunk_1_8Test {
    @BeforeEach
    void setup() {
        WorldManager mock = mock(WorldManager.class);
        when(mock.getBlockColors()).thenReturn(mock(BlockColors.class));
        when(mock.getChunkFactory()).thenReturn(new ChunkFactory());
        when(mock.getDimension()).thenReturn(Dimension.OVERWORLD);
        when(mock.getDimensionRegistry()).thenReturn(mock(DimensionRegistry.class));
        WorldManager.setInstance(mock);

        RegistryManager registryManager = mock(RegistryManager.class);
        when(registryManager.getBlockEntityRegistry()).thenReturn(new BlockEntityRegistry());
        RegistryManager.setInstance(registryManager);

        Config.setInstance(new Config());
        Config.setProtocolVersion(Version.V1_8.protocolVersion);
    }

    @Test
    void parsesSingleSectionColumn() {
        int targetX = 1, targetY = 2, targetZ = 3;
        int blockValue = 1 << 4; // blockId 1 (stone), metadata 0 -> (id << 4) | meta

        // 4096 little-endian shorts of block data; only the target block is non-air
        byte[] blockData = new byte[8192];
        int index = (targetY << 8) | (targetZ << 4) | targetX;
        blockData[index * 2] = (byte) (blockValue & 0xFF);
        blockData[index * 2 + 1] = (byte) ((blockValue >> 8) & 0xFF);

        ByteArrayOutputStream column = new ByteArrayOutputStream();
        column.writeBytes(blockData);
        column.writeBytes(new byte[2048]); // block light
        column.writeBytes(new byte[2048]); // sky light (overworld)
        column.writeBytes(new byte[256]);  // biomes (full chunk)
        byte[] columnData = column.toByteArray();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(1);                                  // full chunk = true
        body.write(0x00);                               // bitmask high byte
        body.write(0x01);                               // bitmask low byte -> section 0 only
        writeVarInt(body, columnData.length);           // data size
        body.writeBytes(columnData);

        DataTypeProvider provider = new DataTypeProvider(body.toByteArray());

        Chunk chunk = new Chunk_1_8(new CoordinateDim2D(0, 0, Dimension.OVERWORLD), Version.V1_8.dataVersion);
        chunk.parse(provider);

        assertThat(chunk.getNumericBlockStateAt(targetX, targetY, targetZ)).isEqualTo(blockValue);
        assertThat(chunk.getNumericBlockStateAt(0, 0, 0)).isEqualTo(0);
        assertThat(chunk.getNumericBlockStateAt(15, 15, 15)).isEqualTo(0);
        assertThat(provider.hasNext()).isFalse();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
