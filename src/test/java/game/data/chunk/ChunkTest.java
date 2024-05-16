package game.data.chunk;

import config.Config;
import config.Version;
import game.data.chunk.version.Chunk_1_17;
import game.data.coordinates.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.palette.BlockColors;
import game.data.dimension.Biome;
import game.data.dimension.BiomeRegistry;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionRegistry;
import game.data.registries.RegistryManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import packets.DataTypeProvider;
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

        Chunk_1_17.setWorldHeight(-63, 384);
        DimensionRegistry codecMock = mock(DimensionRegistry.class);
        Map<String, Biome> biomeMap = new HashMap<>();
        biomeMap.put("minecraft:badlands", new Biome(0));
        biomeMap.put("minecraft:forest", new Biome(1));
        biomeMap.put("minecraft:river", new Biome(2));
        when(codecMock.getBiomeRegistry()).thenReturn(new BiomeRegistry(biomeMap));
        when(mock.getDimensionRegistry()).thenReturn(codecMock);

        RegistryManager registryManager = mock(RegistryManager.class);
        when(registryManager.getBlockEntityRegistry()).thenReturn(new BlockEntityRegistry());
        RegistryManager.setInstance(registryManager);

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

        Chunk_1_17.setWorldHeight(0, 256);
    }

    /**
     * Tests that reading in binary chunk data (as stored in MCA Files), writing it to a network packet and parsing the
     * network packet leads to the same block states. Note that this does not ensure that the client is necessarily able
     * to understand the chunk, just that it is internally consistent.
     */
    private void testForWithLight(int protocolVersion, String dataFile) throws IOException, ClassNotFoundException {
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

        builder = c.toLightPacket();
        DataTypeProvider lightParser = getParser();

        Chunk parsed = ChunkFactory.parseChunk(up, mock);
        assertThat(lightParser.readVarInt()).isEqualTo(0);
        assertThat(lightParser.readVarInt()).isEqualTo(0);
        parsed.updateLight(lightParser);
        assertThat(parsed).isEqualTo(c);
    }

    @Test
    void chunk_1_12() throws IOException, ClassNotFoundException {
        testFor(Version.V1_12.protocolVersion, "chunkdata_1_12");
    }

    @Test
    void chunk_1_13() throws IOException, ClassNotFoundException {
        testFor(Version.V1_13.protocolVersion, "chunkdata_1_13");
    }

    @Test
    void chunk_1_14() throws IOException, ClassNotFoundException {
        testFor(Version.V1_14.protocolVersion, "chunkdata_1_14");
    }

    @Test
    void chunk_1_15() throws IOException, ClassNotFoundException {
        testFor(Version.V1_15.protocolVersion, "chunkdata_1_15");
    }

    @Test
    void chunk_1_16() throws IOException, ClassNotFoundException {
        testFor(Version.V1_16.protocolVersion, "chunkdata_1_16");
    }

    @Test
    void chunk_1_16_light() throws IOException, ClassNotFoundException {
        testFor(Version.V1_16.protocolVersion, "chunkdata_1_16");

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

    @Test
    void chunk_1_17() throws IOException, ClassNotFoundException {
        testForWithLight(Version.V1_17.protocolVersion, "chunkdata_1_17");
    }

    @Test
    void chunk_1_19() throws IOException, ClassNotFoundException {
        testFor(Version.V1_19.protocolVersion, "chunkdata_1_19");
    }
}