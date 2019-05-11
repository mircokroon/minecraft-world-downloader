package game.data;

public class BlockState {
    int id;
    int meta;

    public BlockState(int i) {
        id = i >> 4;
        meta = i & 15;
    }

    @Override
    public String toString() {
        return id + ":" + meta;
    }
}
