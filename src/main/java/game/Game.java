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

import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.nio.file.Paths;

public abstract class Game {
    private static NetworkMode mode = NetworkMode.STATUS;
    private static Dimension dimension = Dimension.OVERWORLD;

    private static DataReader serverBoundDataReader;
    private static DataReader clientBoundDataReader;
    private static EncryptionManager encryptionManager;
    private static CompressionManager compressionManager;
    public static EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }
    public static CompressionManager getCompressionManager() {
        return compressionManager;
    }
    public static void setDimension(Dimension dimension) {
        Game.dimension = dimension;
    }
    public static Dimension getDimension() {
        return dimension;
    }

    private static String host;
    private static String exportDir;
    private static int portRemote;
    private static int portLocal;

    public static void init(Namespace args) {
        host = args.getString("server");
        portRemote = args.getInt("port");
        portLocal = args.getInt("local-port");
        exportDir = args.getString("output");

        File dir = Paths.get(exportDir).toFile();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    public static String getHost() {
        return host;
    }

    public static int getPortRemote() {
        return portRemote;
    }

    public static void startProxy() {
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


    public static String getExportDirectory() {
        return exportDir;
    }
}
