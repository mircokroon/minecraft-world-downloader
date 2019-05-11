import game.Game;
import game.data.WorldManager;

public class Main {
    public static void main(String[] args) {
        WorldManager.startSaveService();
        Game.startProxy();
    }
}
