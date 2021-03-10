package game.data.chunk;

import config.Config;
import game.data.coordinates.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.palette.BlockColors;
import game.data.dimension.Dimension;
import org.junit.jupiter.api.Test;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;
import packets.builder.PacketBuilderAndParserTest;

import java.io.IOException;
import java.io.ObjectInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChunkTest extends PacketBuilderAndParserTest {
    @Override
    public void afterEach() {
    }

    CoordinateDim2D pos = new CoordinateDim2D(0, 0, Dimension.OVERWORLD);
    ChunkBinary cb;

    /**
     * Tests that reading in binary chunk data (as stored in MCA Files), writing it to a network packet and parsing the
     * network packet leads to the same block states. Note that this does not ensure that the client is necessarily able
     * to understand the chunk, just that it is internally consistent.
     */
    private void testFor(int protocolVersion, String dataFile) throws IOException, ClassNotFoundException {
        // set up mock
        WorldManager mock = mock(WorldManager.class);
        when(mock.getBlockColors()).thenReturn(mock(BlockColors.class));
        when(mock.getChunkFactory()).thenReturn(new ChunkFactory());

        WorldManager.setInstance(mock);

        Config.setInstance(new Config());
        Config.setProtocolVersion(protocolVersion);

        ObjectInputStream in = new ObjectInputStream(ChunkTest.class.getClassLoader().getResourceAsStream(dataFile));
        cb = (ChunkBinary) in.readObject();

        Chunk c = cb.toChunk(pos);

        builder = c.toPacket();

        DataTypeProvider parser = getParser();
        CoordinateDim2D coords = new CoordinateDim2D(parser.readInt(), parser.readInt(), pos.getDimension());
        UnparsedChunk up = new UnparsedChunk(coords);
        up.provider = parser;

        assertThat(ChunkFactory.parseChunk(up, mock)).isEqualTo(c);
    }

    @Test
    void chunk_1_12() throws IOException, ClassNotFoundException {
        testFor(340, "chunkdata_1_12");
    }

    @Test
    void chunk_1_13() throws IOException, ClassNotFoundException {
        testFor(404, "chunkdata_1_13");
    }

    @Test
    void chunk_1_14() throws IOException, ClassNotFoundException {
        testFor(498, "chunkdata_1_14");
    }

    @Test
    void chunk_1_15() throws IOException, ClassNotFoundException {
        testFor(578, "chunkdata_1_15");
    }

    @Test
    void chunk_1_16() throws IOException, ClassNotFoundException {
        testFor(751, "chunkdata_1_16");
    }

    @Test
    void chunk_1_16_light() throws IOException, ClassNotFoundException {
        testFor(751, "chunkdata_1_16");

        int trueX = -100;
        int trueZ = 100;

        Chunk src = cb.toChunk(new CoordinateDim2D(trueX, trueZ, Dimension.OVERWORLD));
        byte[] sky = src.getChunkSection(0).getSkyLight();
        sky[0] = 42;
        sky[100] = 24;

        byte[] block = src.getChunkSection(0).getBlockLight();
        block[100] = 42;
        block[0] = 24;

        builder = src.toLightPacket();
        DataTypeProvider provider = getParser();
        int x = provider.readVarInt();
        int z = provider.readVarInt();

        assertThat(x).isEqualTo(trueX);
        assertThat(z).isEqualTo(trueZ);

        Chunk dst = cb.toChunk(pos);

        assertThat(dst.getChunkSection(0).getBlockLight()).isNotEqualTo(block);
        assertThat(dst.getChunkSection(0).getSkyLight()).isNotEqualTo(sky);

        dst.updateLight(provider);

        assertThat(dst.getChunkSection(0).getBlockLight()).isEqualTo(block);
        assertThat(dst.getChunkSection(0).getSkyLight()).isEqualTo(sky);
    }


}