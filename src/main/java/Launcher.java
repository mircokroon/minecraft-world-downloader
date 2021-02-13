import config.Config;
import proxy.ConnectionManager;

public class Launcher {
    public static void main(String[] args) {
        Config.init(args);

        new ConnectionManager().startProxy();
    }
}
