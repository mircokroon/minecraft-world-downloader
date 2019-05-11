package game;

import game.data.Dimension;
import packets.ClientBoundGamePacketBuilder;
import packets.ClientBoundHandshakePacketBuilder;
import packets.ClientBoundLoginPacketBuilder;
import packets.ClientBoundStatusPacketBuilder;
import packets.DataReader;
import packets.ServerBoundGamePacketBuilder;
import packets.ServerBoundLoginPacketBuilder;
import packets.ServerBoundHandshakePacketBuilder;
import packets.ServerBoundStatusPacketBuilder;
import proxy.CompressionManager;
import proxy.EncryptionManager;
import proxy.ProxyServer;

public abstract class Game {
    private static NetworkMode mode = NetworkMode.STATUS;

    private static DataReader serverBoundDataReader;
    private static DataReader clientBoundDataReader;

    public static EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public static CompressionManager getCompressionManager() {
        return compressionManager;
    }

    private static EncryptionManager encryptionManager;

    private static CompressionManager compressionManager;

    private static Dimension dimension = Dimension.OVERWORLD;

    public static void setDimension(Dimension dimension) {
        Game.dimension = dimension;
    }

    public static Dimension getDimension() {
        return dimension;
    }

    private static String host = "localhost";
    private static int portRemote = 25565;


    public static String getHost() {
        return host;
    }

    public static int getPortRemote() {
        return portRemote;
    }

    public static void startProxy() {
        int portLocal = 25570;

        encryptionManager = new EncryptionManager();
        serverBoundDataReader = DataReader.serverBound(encryptionManager);
        clientBoundDataReader = DataReader.clientBound(encryptionManager);
        compressionManager = new CompressionManager();

        setMode(NetworkMode.HANDSHAKE);

        ProxyServer proxy = new ProxyServer(portRemote, portLocal, host);
        proxy.runServer(serverBoundDataReader, clientBoundDataReader, encryptionManager);
    }

    public static NetworkMode getMode() {
        return mode;
    }

    public static void reset() {
        encryptionManager.reset();
        compressionManager.reset();
        serverBoundDataReader.reset();
        clientBoundDataReader.reset();
        setMode(NetworkMode.HANDSHAKE);
    }

    public static void setMode(NetworkMode mode) {
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
                serverBoundDataReader.setBuilder(new ServerBoundGamePacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundGamePacketBuilder());
                break;
            case HANDSHAKE:
                serverBoundDataReader.setBuilder(new ServerBoundHandshakePacketBuilder());
                clientBoundDataReader.setBuilder(new ClientBoundHandshakePacketBuilder());
                break;
        }
    }


}
