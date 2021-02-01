package game;

import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.dimension.Dimension;
import game.data.registries.RegistryLoader;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import game.data.chunk.palette.Palette;
import game.protocol.HandshakeProtocol;
import game.protocol.LoginProtocol;
import game.protocol.Protocol;
import game.protocol.StatusProtocol;
import gui.GuiManager;
import net.sourceforge.argparse4j.inf.Namespace;
import packets.DataReader;
import packets.handler.ClientBoundGamePacketHandler;
import packets.handler.ClientBoundHandshakePacketHandler;
import packets.handler.ClientBoundLoginPacketHandler;
import packets.handler.ClientBoundStatusPacketHandler;
import packets.handler.PacketHandler;
import packets.handler.ServerBoundGamePacketHandler;
import packets.handler.ServerBoundHandshakePacketHandler;
import packets.handler.ServerBoundLoginPacketHandler;
import packets.handler.ServerBoundStatusPacketHandler;
import proxy.CompressionManager;
import proxy.EncryptionManager;
import proxy.ProxyServer;

/**
 * Class the manage the central configuration and set up.
 */
public abstract class Game {
    private static final int DEFAULT_VERSION = 340;
    private static NetworkMode mode = NetworkMode.STATUS;
    private static Dimension dimension = Dimension.OVERWORLD;
    private static Coordinate3D playerPosition = new Coordinate3D(0, 80, 0);

    private static ProtocolVersionHandler versionHandler;
    private static String gameVersion;
    private static int protocolVersion = DEFAULT_VERSION;
    private static int dataVersion;

    private static Namespace args;
    private static DataReader serverBoundDataReader;
    private static DataReader clientBoundDataReader;
    private static EncryptionManager encryptionManager;
    private static CompressionManager compressionManager;
    private static boolean enableWorldGen;
    private static boolean enableSrvLookup;

    public static int getDataVersion() {
        return dataVersion;
    }

    public static String getGameVersion() {
        return gameVersion;
    }

    public static EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public static CompressionManager getCompressionManager() {
        return compressionManager;
    }

    public static Dimension getDimension() {
        return dimension;
    }

    public static void setDimension(Dimension dimension) {
        Game.dimension = dimension;
    }

    public static Coordinate3D getPlayerPosition() {
        return playerPosition;
    }

    public static void setPlayerPosition(Coordinate3D newPos) {
        playerPosition = newPos;
    }

    public static long getSeed() {
        return args.getLong("seed");
    }

    /**
     * Parse arguments from the commandline.
     */
    public static void init(Namespace args) {
        Game.args = args;

        Coordinate2D.setOffset(-args.getInt("center-x"), -args.getInt("center-z"));

        Palette.setMaskBedrock(args.getBoolean("mask-bedrock"));

        WorldManager.setSaveServiceVariables(args.getBoolean("mark-new-chunks"), args.getBoolean("write-chunks"));
        if (args.getBoolean("gui")) {
            GuiManager.showGui();
        }
        enableWorldGen = args.getBoolean("enable-world-gen");
        enableSrvLookup = args.getBoolean("enable-srv-lookup");

        versionHandler = ProtocolVersionHandler.getInstance();
    }

    public static String getExportDirectory() {
        return args.getString("output");
    }

    public static void startProxy() {
        encryptionManager = new EncryptionManager();
        serverBoundDataReader = DataReader.serverBound(encryptionManager);
        clientBoundDataReader = DataReader.clientBound(encryptionManager);
        compressionManager = new CompressionManager();

        setMode(NetworkMode.HANDSHAKE);

        ProxyServer proxy = new ProxyServer(getPortRemote(), getPortLocal(), getHost());
        proxy.runServer(serverBoundDataReader, clientBoundDataReader);
    }

    public static int getPortRemote() {
        return args.getInt("port");
    }

    private static int getPortLocal() {
        return args.getInt("local-port");
    }

    public static String getHost() {
        return args.getString("server");
    }

    public static Protocol getGameProtocol() {
        Protocol p = versionHandler.getProtocolByProtocolVersion(protocolVersion);
        Game.dataVersion = p.getDataVersion();
        Game.gameVersion = p.getVersion();

        new Thread(() -> loadVersionRegistries(p)).start();

        System.out.println("Using protocol of game version " + p.getVersion() + " (" + protocolVersion + ")");
        return p;
    }

    private static void loadVersionRegistries(Protocol p) {
        try {
            RegistryLoader loader = RegistryLoader.forVersion(p.getVersion());

            WorldManager.setEntityMap(loader.generateEntityNames());
            WorldManager.setMenuRegistry(loader.generateMenuRegistry());
            WorldManager.setItemRegistry(loader.generateItemRegistry());

            WorldManager.startSaveService();
            ChunkFactory.startChunkParserService();

            loader.clean();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static NetworkMode getMode() {
        return mode;
    }

    public static void setMode(NetworkMode mode) {
        Game.mode = mode;

        switch (mode) {
            case STATUS:
                PacketHandler.setProtocol(new StatusProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundStatusPacketHandler());
                clientBoundDataReader.setPacketHandler(new ClientBoundStatusPacketHandler());
                break;
            case LOGIN:
                PacketHandler.setProtocol(new LoginProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundLoginPacketHandler());
                clientBoundDataReader.setPacketHandler(new ClientBoundLoginPacketHandler());
                break;
            case GAME:
                PacketHandler.setProtocol(getGameProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundGamePacketHandler());
                clientBoundDataReader.setPacketHandler(new ClientBoundGamePacketHandler());
                break;
            case HANDSHAKE:
                PacketHandler.setProtocol(new HandshakeProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundHandshakePacketHandler());
                clientBoundDataReader.setPacketHandler(new ClientBoundHandshakePacketHandler());
                break;
        }
    }

    /**
     * Reset the connection when its lost.
     */
    public static void reset() {
        encryptionManager.reset();
        compressionManager.reset();
        serverBoundDataReader.reset();
        clientBoundDataReader.reset();
        setMode(NetworkMode.HANDSHAKE);
    }

    public static String getGamePath() {
        return args.getString("minecraft");
    }

    public static int getProtocolVersion() {
        return protocolVersion;
    }

    public static void setProtocolVersion(int protocolVersion) {
        Game.protocolVersion = protocolVersion;
    }

    public static int getRenderDistance() {
        return args.getInt("render-distance");
    }

    public static boolean markUnsavedChunks() {
        return args.get("mark-unsaved-chunks");
    }

    public static boolean isWorldGenEnabled() {
        return enableWorldGen;
    }

    public static boolean isSrvLookupEnabled() {
        return enableSrvLookup;
    }
}
