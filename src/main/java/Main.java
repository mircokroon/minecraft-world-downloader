import game.Game;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;

public class Main {
    public static void main(String[] args) {
        ChunkFactory.startChunkParserService();
        WorldManager.startSaveService();
        Game.startProxy();
    }
}
