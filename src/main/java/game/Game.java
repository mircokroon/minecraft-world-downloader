package game;

import packets.ClientBoundGamePacketBuilder;
import packets.ClientBoundHandshakePacketBuilder;
import packets.ClientBoundLoginPacketBuilder;
import packets.ClientBoundStatusPacketBuilder;
import packets.DataReader;
import packets.ServerBoundGamePackterBuilder;
import packets.ServerBoundLoginPacketBuilder;
import packets.ServerBoundHandshakePacketBuilder;
import packets.ServerBoundStatusPacketBuilder;
import proxy.ProxyServer;

public abstract class Game {
    private static NetworkMode mode = NetworkMode.STATUS;

    private static DataReader serverBoundDataReader;
    private static DataReader clientBoundDataReader;

    public static void startProxy() {
        String host = "localhost";
        int portRemote = 25565;
        int portLocal = 25570;

        serverBoundDataReader = new DataReader();
        clientBoundDataReader = new DataReader();

        setMode(NetworkMode.HANDSHAKE);

        ProxyServer proxy = new ProxyServer(portRemote, portLocal, host);
        proxy.runServer(serverBoundDataReader, clientBoundDataReader);
    }

    public static NetworkMode getMode() {
        return mode;
    }

    public static void setMode(NetworkMode mode) {
        System.out.println(mode);
        Game.mode = mode;

        switch (mode) {
            case STATUS:
                serverBoundDataReader.setBuilder(new ServerBoundStatusPacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundStatusPacketBuilder());
                break;
            case LOGIN:
                serverBoundDataReader.setBuilder(new ServerBoundLoginPacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundLoginPacketBuilder());
                break;
            case GAME:
                serverBoundDataReader.setBuilder(new ServerBoundGamePackterBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundGamePacketBuilder());
                break;
            case HANDSHAKE:
                serverBoundDataReader.setBuilder(new ServerBoundHandshakePacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundStatusPacketBuilder());
                break;
        }
    }


}
