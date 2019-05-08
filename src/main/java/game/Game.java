package game;

import packets.ClientBoundStatusPacketBuilder;
import packets.DataReader;
import packets.ServerBoundStatusPacketBuilder;
import proxy.ProxyServer;

public abstract class Game {
    private static int mode = 1;

    private static DataReader serverBoundDataReader;
    private static DataReader clientBoundDataReader;

    public static void startProxy() {
        String host = "localhost";
        int portRemote = 25565;
        int portLocal = 25570;

        serverBoundDataReader = new DataReader();
        clientBoundDataReader = new DataReader();

        ProxyServer proxy = new ProxyServer(portRemote, portLocal, host);
        proxy.runServer(serverBoundDataReader, clientBoundDataReader);
    }

    public static int getMode() {
        return mode;
    }

    public static void setMode(int mode) {
        Game.mode = mode;

        switch (mode) {
            case 1:
                serverBoundDataReader.setBuilder(new ServerBoundStatusPacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundStatusPacketBuilder());
        }
    }


}
