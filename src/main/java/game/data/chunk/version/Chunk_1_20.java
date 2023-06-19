package game.data.chunk.version;

import game.data.coordinates.CoordinateDim2D;
import packets.DataTypeProvider;
import packets.builder.PacketBuilder;

public class Chunk_1_20 extends Chunk_1_18 {
    public Chunk_1_20(CoordinateDim2D location, int version) {
        super(location, version);
    }

    /**
     * Trusted edges parameter for lighting on chunk edges was removed in 1.20
     */
    @Override
    void parseLightEdgesTrusted(DataTypeProvider provider) {

    }

    @Override
    void writeLightEdgesTrusted(PacketBuilder packet) {

    }
}
