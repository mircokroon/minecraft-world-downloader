package game.data.chunk;

public class BlockState {
    int id;
    int meta;
    int state;

    public BlockState(int i) {
        id = i >>> 4;
        meta = i & 0x0F;
        state = i;
    }

    @Override
    public String toString() {
        return id + ":" + meta;
    }
}
