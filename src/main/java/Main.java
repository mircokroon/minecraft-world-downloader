import game.Game;
import proxy.ClientAuthenticator;

public class Main {
    public static void main(String[] args) {
        new ClientAuthenticator();
        Game.startProxy();
    }
}
