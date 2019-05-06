import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        String host = "localhost";
        int portRemote = 25565;
        int portLocal = 25570;

        ProxyServer proxy = new ProxyServer(portRemote, portLocal, host);
        proxy.runServer((b) -> System.out.println(Arrays.toString(b)), (b) -> {});
    }
}
