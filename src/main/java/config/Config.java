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
import proxy.ConnectionManager;
import proxy.auth.AuthDetails;

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

    private boolean isStarted = false;
    private boolean guiOnlyMode = false;

    public static void setInstance(Config config) {
        instance = config;
    }

    public static void init(String[] args) {
        instance = new Config();
        CmdLineParser parser = new CmdLineParser(instance);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());

            instance.showHelp = true;
        }

        if (instance.showHelp) {
            System.out.println("When running this application without the -s parameter, the settings UI will be \n" +
                                "shown on startup. When running with --no-gui, the -s parameter is required.\n");

            System.out.println("Available parameters:");
            parser.printUsage(System.out);
            System.exit(1);
        }

        instance.settingsComplete();
    }

    public static ConnectionDetails getConnectionDetails() {
        return instance.connectionDetails;
    }

    public static void setProtocolVersion(int protocolVersion) {
        instance.protocolVersion = protocolVersion;
    }

    public static boolean inGuiMode() {
        return instance.guiOnlyMode;
    }

    public static Config getInstance() {
        return instance;
    }

    public void settingsComplete() {
        GuiManager.setConfig(this);

        if (server == null) {
            handleGuiOnlyMode();

            GuiManager.loadSceneSettings();
            return;
        }

        WorldManager.getInstance().setSaveServiceVariables(markNewChunks, writeChunks());
        WorldManager.getInstance().updateExtendedRenderDistance(extendedRenderDistance);

        if (isStarted) {
            return;
        }

        isStarted = true;

        versionHandler = ProtocolVersionHandler.getInstance();
        connectionDetails = new ConnectionDetails(server, portRemote, portLocal, !disableSrvLookup);

        if (!disableGui) {
            GuiManager.loadSceneMap();
        }

        new ConnectionManager().startProxy();

    }

    private void handleGuiOnlyMode() {
        guiOnlyMode = true;
        if (System.console() != null) {
            return;
        }

        if (!devMode && !forceConsoleOutput) {
            System.out.println("Application seems to be running without console. Redirecting error output to GUI. " +
                    "If this is not desired, run with --force-console.");

            GuiManager.redirectErrorOutput();
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isValid() {
        return this.server != null;
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
     * Get the contents of the Minecraft launcher_profiles.json from the given installation path.
     * @return the contents of the file
     */
    public static String getMinecraftPath() {
        String path = instance.minecraftDir;

        // handle common %APPDATA% env variable for Windows
        if (path.toUpperCase().contains("%APPDATA%") && System.getenv("appdata") != null) {
            String appdataPath = System.getenv("appdata").replace("\\", "\\\\");
            path = path.replaceAll("(?i)%APPDATA%", appdataPath);
        }

        return path;
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


    @Option(name = "--help", aliases = {"-h", "help", "-help", "--h"},
            usage = "Show this help message.")
    public boolean showHelp;

    // parameters
    @Option(name = "--server", aliases = "-s",
            usage = "The address of the remote server to connect to. Hostname or IP address (without port).")
    public String server;

    @Option(name = "--token", aliases = "-t",
            usage = "Minecraft access token. Found in launcher_accounts.json by default.")
    public String accessToken;

    @Option(name = "--username", aliases = "-u",
            usage = "Your Minecraft username.")
    public String username;

    @Option(name = "--port", aliases = "-p",
            usage = "The port on which the remote server runs.")
    public int portRemote = 25565;

    @Option(name = "--local-port", aliases = "-l",
            usage = "The port on which the world downloader's server will run.")
    public int portLocal = 25565;

    @Option(name = "--extended-render-distance", aliases = "-r",
            usage = "When set, send downloaded chunks to client to extend render distance to given amount.")
    public int extendedRenderDistance = 0;

    @Option(name = "--measure-render-distance", depends = "--extended-render-distance",
            usage = "When set, ignores the server's render distance value and measure it by looking at loaded chunks.")
    public boolean measureRenderDistance = false;

    @Option(name = "--seed",
            usage = "Numeric level seed for output world.")
    public long levelSeed = 0;

    @Option(name = "--minecraft-dir", aliases = "-m",
            usage = "Path to your Minecraft installation, used to retrieve Minecraft authentication details.")
    public String minecraftDir = getDefaultPath();

    @Option(name = "--output", aliases = "-o",
            usage = "The world output directory. If the world already exists, it will be updated.")
    public String worldOutputDir = "world";

    @Option(name = "--center-x", depends = "--center-z",
            usage = "Offsets output world. Given center X coordinate will be put at world origin (0, 0).")
    public int centerX = 0;

    @Option(name = "--center-z", depends = "--center-x",
            usage = "Offsets output world. Given center Z coordinate will be put at world origin (0, 0).")
    public int centerZ = 0;

    @Option(name = "--overview-zoom", depends = "-z",
            usage = "Render distance of (in chunks) of the overview map. Can also be changed by scrolling on GUI.")
    public int zoomLevel = 75;

    @Option(name = "--no-gui", depends = "--server",
            usage = "Disable the GUI")
    public boolean disableGui = false;

    @Option(name = "--mark-new-chunks",
            usage = "Mark new chunks in an orange outline.")
    public boolean markNewChunks = false;

    @Option(name = "--disable-chunk-saving",
            usage = "Disable writing chunks to disk, mostly for debugging purposes.")
    public  boolean disableWriteChunks = false;

    @Option(name = "--disable-world-gen",
            usage = "Set world type to a superflat void to prevent new chunks from being added.")
    public boolean disableWorldGen = false;

    @Option(name = "--disable-srv-lookup",
            usage = "Disable checking for true address using DNS service records")
    public boolean disableSrvLookup = false;

    @Option(name = "--disable-mark-unsaved",
            usage = "Disable marking unsaved chunks in red on the map")
    public boolean disableMarkUnsavedChunks = false;

    @Option(name = "--dev-mode",
            usage = "Enable developer mode")
    private boolean devMode = false;

    @Option(name = "--force-console",
            usage = "Never redirect console output to GUI")
    private boolean forceConsoleOutput = false;

    // getters
    public static int getExtendedRenderDistance() {
        return instance.extendedRenderDistance;
    }

    public static boolean doMeasureRenderDistance() {
        return instance.measureRenderDistance;
    }

    public static long getLevelSeed() {
        return instance.levelSeed;
    }

    public static String getWorldOutputDir() {
        return instance.worldOutputDir;
    }

    public static int getZoomLevel() {
        return instance.zoomLevel;
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

    public static AuthDetails getAuthDetails() {
        return AuthDetails.fromUsername(instance.username, instance.accessToken);
    }

    // inverted boolean getters
    public static boolean isWorldGenEnabled() {
        return !instance.disableWorldGen;
    }

    public static boolean markUnsavedChunks() {
        return !instance.disableMarkUnsavedChunks;
    }

    // setters
    public static void setZoomLevel(int val) {
        instance.zoomLevel = val;
    }
}

