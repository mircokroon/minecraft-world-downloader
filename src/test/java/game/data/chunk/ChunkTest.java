package game.data.chunk;

import config.Config;
import game.data.coordinates.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.palette.BlockColors;
import game.data.dimension.Dimension;
import org.junit.jupiter.api.Test;
import packets.builder.PacketBuilderAndParserTest;

import java.io.IOException;
import java.io.ObjectInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChunkTest extends PacketBuilderAndParserTest {
    @Override
    public void afterEach() {
    }

    /**
     * Tests that reading in binary chunk data (as stored in MCA Files), writing it to a network packet and parsing the
     * network packet leads to the same block states. Note that this does not ensure that the client is necessarily able
     * to understand the chunk, just that it is internally consistent.
     */
    private void testFor(int protocolVersion, String dataFile) throws IOException, ClassNotFoundException {
        // set up mock
        WorldManager mock = mock(WorldManager.class);
        when(mock.getBlockColors()).thenReturn(mock(BlockColors.class));

        WorldManager.setInstance(mock);

        Config.setProtocolVersion(protocolVersion);

        // load chunk
        ObjectInputStream in = new ObjectInputStream(ChunkTest.class.getClassLoader().getResourceAsStream(dataFile));
        ChunkBinary chunkBinary = (ChunkBinary) in.readObject();

        CoordinateDim2D pos = new CoordinateDim2D(0, 0, Dimension.OVERWORLD);
        Chunk c = chunkBinary.toChunk(pos);

        builder = c.toPacket();

        assertThat(ChunkFactory.parseChunk(new ChunkParserPair(getParser(), pos.getDimension()), mock)).isEqualTo(c);
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


}