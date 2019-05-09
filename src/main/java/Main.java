import java.util.Arrays;

public class Main {

    int mode = -1;

    public static void main(String[] args) {
        String host = "localhost";
        int portRemote = 25565;
        int portLocal = 25570;

        ProxyServer proxy = new ProxyServer(portRemote, portLocal, host);
        proxy.runServer(new PacketHandler("server"), new PacketHandler("client"));
    }
}
