package game;

import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.dimension.Dimension;
import game.data.registries.RegistryLoader;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import game.data.chunk.palette.Palette;
import game.protocol.Protocol;
import gui.GuiManager;
import net.sourceforge.argparse4j.inf.Namespace;
import packets.builder.PacketBuilder;
import packets.lib.ByteQueue;
import proxy.ConnectionDetails;

import java.util.function.Consumer;

/**
 * Class the manage the central configuration and set up.
 */
public abstract class Config {
    private static final int DEFAULT_VERSION = 340;
    private static Dimension dimension = Dimension.OVERWORLD;
    private static Coordinate3D playerPosition = new Coordinate3D(0, 80, 0);
    private static Consumer<PacketBuilder> injector;

    private static ProtocolVersionHandler versionHandler;
    private static String gameVersion;
    private static int protocolVersion = DEFAULT_VERSION;
    private static int dataVersion;

    private static Namespace args;
    private static boolean enableWorldGen;

    public static ConnectionDetails connectionDetails;

    // basic getters
    public static int getDataVersion() {
        return dataVersion;
    }

    public static String getGameVersion() {
        return gameVersion;
    }

    public static Dimension getDimension() {
        return dimension;
    }

    public static Coordinate3D getPlayerPosition() {
        return playerPosition;
    }

    public static int getProtocolVersion() {
        return protocolVersion;
    }

    public static boolean isWorldGenEnabled() {
        return enableWorldGen;
    }

    // args getters
    public static String getGamePath() {
        return args.getString("minecraft");
    }

    public static int getRenderDistance() {
        return args.getInt("render-distance");
    }

    public static boolean markUnsavedChunks() {
        return args.get("mark-unsaved-chunks");
    }

    public static long getSeed() {
        return args.getLong("seed");
    }


    // setters
    public static void setProtocolVersion(int protocolVersion) {
        Config.protocolVersion = protocolVersion;
    }

    public static void setDimension(Dimension dimension) {
        Config.dimension = dimension;
    }

    public static void setPlayerPosition(Coordinate3D newPos) {
        Config.playerPosition = newPos;
    }

    /**
     * Parse arguments from the commandline.
     */
    public static void init(Namespace args) {
        Config.args = args;

        Coordinate2D.setOffset(-args.getInt("center-x"), -args.getInt("center-z"));

        Palette.setMaskBedrock(args.getBoolean("mask-bedrock"));

        WorldManager.setSaveServiceVariables(args.getBoolean("mark-new-chunks"), args.getBoolean("write-chunks"));
        if (args.getBoolean("gui")) {
            GuiManager.showGui();
        }
        enableWorldGen = args.getBoolean("enable-world-gen");

        versionHandler = ProtocolVersionHandler.getInstance();

        connectionDetails = new ConnectionDetails(
                args.getString("server"),
                args.getInt("port"),
                args.getInt("local-port"),
                args.getBoolean("enable-srv-lookup")
        );
    }

    public static String getExportDirectory() {
        return args.getString("output");
    }

    public static ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }

    public static Protocol getGameProtocol() {
        Protocol p = versionHandler.getProtocolByProtocolVersion(protocolVersion);
        Config.dataVersion = p.getDataVersion();
        Config.gameVersion = p.getVersion();

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

    /**
     * Packet injector allows new packets to be sent to the client.
     */
    public static void registerPacketInjector(Consumer<PacketBuilder> injector) {
        Config.injector = injector;
    }

    public static Consumer<PacketBuilder> getPacketInjector() {
        return injector;
    }
}
