package config;

import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import game.data.registries.RegistryLoader;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import gui.GuiManager;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import packets.builder.PacketBuilder;
import proxy.ConnectionDetails;

import java.nio.file.Paths;
import java.util.function.Consumer;

public class Config {
    private static final int DEFAULT_VERSION = 340;
    private static Consumer<PacketBuilder> injector;
    private static Config instance;

    private ProtocolVersionHandler versionHandler;
    private String gameVersion;
    private int protocolVersion = DEFAULT_VERSION;
    private int dataVersion;
    private ConnectionDetails connectionDetails;


    public static void init(String[] args) {
        instance = new Config();
        CmdLineParser parser = new CmdLineParser(instance);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
        instance.afterInit();
    }

    public static ConnectionDetails getConnectionDetails() {
        return instance.connectionDetails;
    }

    public static void setProtocolVersion(int protocolVersion) {
        instance.protocolVersion = protocolVersion;
    }

    private void afterInit() {
        if (server == null) {
            GuiManager.showGUI(true);
            System.out.println("Hello");
        }

        versionHandler = ProtocolVersionHandler.getInstance();
        connectionDetails = new ConnectionDetails(server, remotePort, portLocal, !disableSrvLookup);

        // TODO: fix
        // Coordinate2D.setOffset(-args.getInt("center-x"), -args.getInt("center-z"));
        // Palette.setMaskBedrock(args.getBoolean("mask-bedrock"));

        WorldManager.getInstance().setSaveServiceVariables(markNewChunks, writeChunks());
        if (!disableGui) {
            GuiManager.showGUI(false);
        }
    }

    /**
     * Get the platform-specific default path for the Minecraft installation directory.
     * @return the path as a string
     */
    private static String getDefaultPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return Paths.get("%appdata%", ".minecraft").toString();
        } else if (SystemUtils.IS_OS_LINUX) {
            return Paths.get(System.getProperty("user.home"), ".minecraft").toString();
        } else if (SystemUtils.IS_OS_UNIX) {
            return Paths.get("/Users/", System.getProperty("user.name"), "/Library/Application Support/minecraft").toString();
        } else {
            return ".minecraft";
        }
    }

    public static Protocol getGameProtocol() {
        Protocol p = instance.versionHandler.getProtocolByProtocolVersion(instance.protocolVersion);
        instance.dataVersion = p.getDataVersion();
        instance.gameVersion = p.getVersion();

        new Thread(() -> loadVersionRegistries(p)).start();

        System.out.println("Using protocol of game version " + p.getVersion() + " (" + instance.protocolVersion + ")");
        return p;
    }

    private static void loadVersionRegistries(Protocol p) {
        try {
            RegistryLoader loader = RegistryLoader.forVersion(p.getVersion());

            WorldManager.getInstance().setEntityMap(loader.generateEntityNames());
            WorldManager.getInstance().setMenuRegistry(loader.generateMenuRegistry());
            WorldManager.getInstance().setItemRegistry(loader.generateItemRegistry());

            WorldManager.getInstance().startSaveService();
            ChunkFactory.startChunkParserService();

            loader.clean();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean writeChunks() {
        return !disableWriteChunks;
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

    // parameters
    @Option(name = "--server", aliases = "-s",
            usage = "The address of the remote server to connect to. Hostname or IP address (without port).")
    String server;

    @Option(name = "--port", aliases = "-p",
            usage = "The port on which the remote server runs.")
    int remotePort = 25565;

    @Option(name = "--local-port", aliases = "-l",
            usage = "The port on which the world downloader's server will run.")
    int portLocal = 25565;

    @Option(name = "--extended-render-distance", aliases = "-r",
            usage = "When set, send downloaded chunks to client to extend render distance to given amount.")
    int extendedRenderDistance = 0;

    @Option(name = "--measure-render-distance", depends = "--extended-render-distance",
            usage = "When set, ignores the server's render distance value and measure it by looking at loaded chunks.")
    boolean measureRenderDistance = false;

    @Option(name = "--seed",
            usage = "Numeric level seed for output world.")
    long levelSeed = 0;

    @Option(name = "--minecraft-dir", aliases = "-m",
            usage = "Path to your Minecraft installation, used to retrieve Minecraft authentication details.")
    String minecraftDir = getDefaultPath();

    @Option(name = "--output", aliases = "-o",
            usage = "The world output directory. If the world already exists, it will be updated.")
    String worldOutputDir = "world";

    @Option(name = "--center-x", depends = "--center-z",
            usage = "Offsets output world. Given center X coordinate will be put at world origin (0, 0).")
    int centerX = 0;

    @Option(name = "--center-z", depends = "--center-x",
            usage = "Offsets output world. Given center Z coordinate will be put at world origin (0, 0).")
    int centerZ = 0;

    @Option(name = "--overview-zoom", depends = "-z",
            usage = "Render distance of (in chunks) of the overview map. Can also be changed by scrolling on GUI.")
    int zoomLevel = 75;

    @Option(name = "--no-gui", depends = "--server",
            usage = "Disable the GUI")
    boolean disableGui = false;

    @Option(name = "--mark-new-chunks",
            usage = "Mark new chunks in an orange outline.")
    boolean markNewChunks = false;

    @Option(name = "--disable-chunk-saving",
            usage = "Disable writing chunks to disk, mostly for debugging purposes.")
    boolean disableWriteChunks = false;

    @Option(name = "--disable-world-gen",
            usage = "Set world type to a superflat void to prevent new chunks from being added.")
    boolean disableWorldGen = false;

    @Option(name = "--disable-srv-lookup",
            usage = "Disable checking for true address using DNS service records")
    boolean disableSrvLookup = false;

    @Option(name = "--disable-mark-unsaved",
            usage = "Disable marking unsaved chunks in red on the map")
    boolean disableMarkUnsavedChunks = false;

    @Option(name = "--dev-mode",
            usage = "Enable developer mode")
    boolean devMode = false;


    // getters
//    public static String getServer() {
//        return instance.server;
//    }
//
//    public static int getRemotePort() {
//        return instance.remotePort;
//    }
//
//    public static int getPortLocal() {
//        return instance.portLocal;
//    }

    public static int getExtendedRenderDistance() {
        return instance.extendedRenderDistance;
    }

    public static boolean doMeasureRenderDistance() {
        return instance.measureRenderDistance;
    }

    public static long getLevelSeed() {
        return instance.levelSeed;
    }

    public static String getMinecraftDir() {
        return instance.minecraftDir;
    }

    public static String getWorldOutputDir() {
        return instance.worldOutputDir;
    }

    public static int getCenterX() {
        return instance.centerX;
    }

    public static int getCenterZ() {
        return instance.centerZ;
    }

    public static int getZoomLevel() {
        return instance.zoomLevel;
    }

    public static boolean isDisableGui() {
        return instance.disableGui;
    }

    public static boolean isMarkNewChunks() {
        return instance.markNewChunks;
    }

    public static boolean isDisableWriteChunks() {
        return instance.disableWriteChunks;
    }

    public static boolean isDisableSrvLookup() {
        return instance.disableSrvLookup;
    }

    public static boolean isDisableMarkUnsavedChunks() {
        return instance.disableMarkUnsavedChunks;
    }

    public static boolean isInDevMode() {
        return instance.devMode;
    }

    public static int getDataVersion() {
        return instance.dataVersion;
    }

    public static String getGameVersion() {
        return instance.gameVersion;
    }

    public static int getProtocolVersion() {
        return instance.protocolVersion;
    }

    // inverted boolean getters
    public static boolean isWorldGenEnabled() {
        return !instance.disableWorldGen;
    }

    public static boolean markUnsavedChunks() {
        return !instance.disableMarkUnsavedChunks;
    }

}

