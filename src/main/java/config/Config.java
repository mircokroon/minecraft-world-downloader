package config;

import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import game.data.WorldManager;
import game.data.registries.RegistryLoader;
import game.data.registries.RegistryManager;
import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;
import gui.GuiManager;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import packets.builder.PacketBuilder;
import proxy.ConnectionDetails;
import proxy.ConnectionManager;
import proxy.PacketInjector;
import proxy.auth.AuthDetails;
import proxy.auth.AuthenticationMethod;
import proxy.auth.MicrosoftAuthHandler;
import util.LocalDateTimeAdapter;
import util.PathUtils;

public class Config {
    private static final int DEFAULT_VERSION = 340;
    private static Path configPath;

    private static PacketInjector injector;
    private static Config instance;

    // fields marked transient so they are not written to JSON file
    private transient ProtocolVersionHandler versionHandler;
    private transient String gameVersion;
    private transient int protocolVersion = DEFAULT_VERSION;
    private transient int dataVersion;
    private transient ConnectionDetails connectionDetails;

    private transient boolean isStarted = false;
    private transient boolean guiOnlyMode = true;

    private transient boolean debugWriteChunkNbt;
    private transient boolean debugTrackEvents = false;
    private transient VersionReporter versionReporter;

    private MicrosoftAuthHandler microsoftAuth;
    private AuthDetails manualAuth;
    private AuthenticationMethod authMethod = AuthenticationMethod.AUTOMATIC;

    public Config() {
        this.versionReporter = new VersionReporter(0);
    }

    public static void setInstance(Config config) {
        instance = config;
    }

    /**
     * Try to read config from file if it exists, otherwise return a new Config object.
     */
    private static Config createConfig() {
        try {
            File file = configPath.toFile();
            if (file.exists() && file.isFile()) {
                Config config = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create()
                    .fromJson(new JsonReader(new FileReader(file)), Config.class);

                return Objects.requireNonNullElseGet(config, () -> new Config());
            }
        } catch (Exception ex) {
            System.out.println("Cannot read " + configPath.toString());
            ex.printStackTrace();
        }
        return new Config();
    }

    public static void init(String[] args) {
        configPath = PathUtils.toPath("cache", "config.json");

        instance = createConfig();
        CmdLineParser parser = new CmdLineParser(instance);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
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

        if (instance.clearSettings) {
            clearSettings();
            System.exit(1);
        }

        instance.settingsComplete();
    }

    public static ConnectionDetails getConnectionDetails() {
        return instance.connectionDetails;
    }

    public static void setProtocolVersion(int protocolVersion) {
        instance.protocolVersion = protocolVersion;
        instance.versionReporter = new VersionReporter(protocolVersion);

        try {
            WorldManager.getInstance().loadLevelData();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean inGuiMode() {
        return instance.guiOnlyMode;
    }

    public static Config getInstance() {
        return instance;
    }

    public static int getCenterX() {
        return instance.centerX;
    }
    public static int getCenterZ() {
        return instance.centerZ;
    }

    public static boolean writeChunksAsNbt() {
        return instance.debugWriteChunkNbt;
    }

    public static void toggleWriteChunkNbt() {
        instance.debugWriteChunkNbt = !instance.debugWriteChunkNbt;
    }

    public static void disableSettingsGui() {
        instance.guiOnlyMode = false;
    }

    public static boolean trackEvents() {
        return instance.debugTrackEvents;
    }

    public static String getUsername() {
        return instance.username;
    }

    public static void handleErrorOutput() {
        instance.handleGuiOnlyMode();
    }

    public boolean startWithSettings() {
        return guiOnlyMode;
    }


    public void settingsComplete() {
        GuiManager.setConfig(this);

        if (guiOnlyMode && !GuiManager.isStarted()) {
            GuiManager.loadSceneSettings();
            return;
        }

        // auth
        boolean hasAccessToken = this.accessToken != null && !this.accessToken.equals("");
        if (hasAccessToken) {
            this.manualAuth = AuthDetails.fromAccessToken(accessToken);
            this.authMethod = AuthenticationMethod.MANUAL;
        }

        // round to regions
        centerX = (centerX >> 9) << 9;
        centerZ = (centerZ >> 9) << 9;

        WorldManager.getInstance().setWorldManagerVariables(markNewChunks, writeChunks());
        WorldManager.getInstance().updateExtendedRenderDistance(extendedRenderDistance);

        writeSettings();

        if (isStarted) {
            return;
        }

        isStarted = true;

        versionHandler = ProtocolVersionHandler.getInstance();
        connectionDetails = new ConnectionDetails(server, portLocal, !disableSrvLookup);

        if (!disableGui) {
            GuiManager.loadSceneMap();
        }

        new ConnectionManager().startProxy();
    }

    private void writeSettings() {
        try {
            // clear other auth settings
            switch (authMethod) {
                case AUTOMATIC -> {
                    manualAuth = null;
                    microsoftAuth = null;
                }
                case MICROSOFT -> manualAuth = null;
                case MANUAL -> microsoftAuth = null;
            }

            String contents = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting().create().toJson(this);
            Files.createDirectories(configPath.getParent());
            Files.write(configPath, Collections.singleton(contents));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void save() {
        instance.writeSettings();
    }

    public static void clearSettings() {
        try {
            if (configPath.toFile().exists()) {
                configPath.toFile().deleteOnExit();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Unable to delete settings.");
        }
    }

    private void handleGuiOnlyMode() {
        if (System.console() != null) {
            return;
        }

        if (!devMode && !forceConsoleOutput) {
            System.out.println("Application seems to be running without console. Redirecting error output to GUI. " +
                    "If this is not desired, run with --force-console.");

            Platform.runLater(GuiManager::redirectErrorOutput);
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Get the platform-specific default path for the Minecraft installation directory.
     * @return the path as a string
     */
    public static String getDefaultMinecraftPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            String path = Paths.get("%appdata%", ".minecraft").toString();

            // handle common %APPDATA% env variable for Windows
            if (path.toUpperCase().contains("%APPDATA%") && System.getenv("appdata") != null) {
                String appdataPath = System.getenv("appdata").replace("\\", "\\\\");
                path = path.replaceAll("(?i)%APPDATA%", appdataPath);
            }

            return path;
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
        instance.versionReporter = new VersionReporter(instance.protocolVersion);

        new Thread(() -> loadVersionRegistries(p)).start();

        System.out.println("Using protocol of game version " + p.getVersion() + " (" + instance.protocolVersion + ")");
        return p;
    }

    private static void loadVersionRegistries(Protocol p) {
        try {
            RegistryLoader loader = RegistryLoader.forVersion(p.getVersion());
            if (loader == null) { return; }

            WorldManager.getInstance().setEntityMap(loader.generateEntityNames());

            RegistryManager.getInstance().setRegistries(loader);

            WorldManager.getInstance().startSaveService();

            loader.clean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean writeChunks() {
        return !disableWriteChunks;
    }

    /**
     * Packet injector allows new packets to be sent to the client.
     */
    public static void registerPacketInjector(PacketInjector injector) {
        Config.injector = injector;
    }

    public static PacketInjector getPacketInjector() {
        return injector;
    }


    @Option(name = "--help", aliases = {"-h", "help", "-help", "--h"},
            usage = "Show this help message.")
    public transient boolean showHelp;

    // parameters
    @Option(name = "--server", aliases = "-s", handler = ServerHandler.class,
            usage = "The address of the remote server to connect to. Hostname or IP address (without port).")
    public String server;

    @Option(name = "--token", aliases = "-t",
            usage = "Minecraft access token. Found in launcher_accounts.json by default.")
    public transient String accessToken;

    @Option(name = "--username", aliases = "-u",
            usage = "Your Minecraft username.")
    public transient String username;

    @Option(name = "--local-port", aliases = "-l",
            usage = "The port on which the world downloader's server will run.")
    public int portLocal = 25565;

    @Option(name = "--extended-render-distance", aliases = "-r",
            usage = "When set, send downloaded chunks to client to extend render distance to given amount.")
    public int extendedRenderDistance = 0;

    @Option(name = "--seed",
            usage = "Numeric level seed for output world.")
    public long levelSeed = 0;

    @Option(name = "--output", aliases = "-o",
            usage = "The world output directory. If the world already exists, it will be updated.")
    public String worldOutputDir = "world";

    @Option(name = "--center-x", depends = "--center-z",
            usage = "Offsets output world. Given center X coordinate will be put at world origin (0, 0). Rounded to multiples of 512 blocks.")
    public int centerX = 0;

    @Option(name = "--center-z", depends = "--center-x",
            usage = "Offsets output world. Given center Z coordinate will be put at world origin (0, 0). Rounded to multiples of 512 blocks.")
    public int centerZ = 0;

    @Option(name = "--render-players",
            usage = "Show other players in the overview map.")
    public boolean renderOtherPlayers = false;

    @Option(name = "--no-gui", depends = "--server",
            usage = "Disable the GUI")
    public transient boolean disableGui = false;

    @Option(name = "--mark-new-chunks",
            usage = "Mark new chunks in an orange outline.")
    public transient boolean markNewChunks = false;

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

    @Option(name = "--mark-old-chunks",
        usage = "Grey out old chunks on the map")
    public boolean markOldChunks = true;

    @Option(name = "--ignore-block-changes",
            usage = "Ignore changes to chunks after they have been loaded.")
    public boolean ignoreBlockChanges = false;

    @Option(name = "--dev-mode",
            usage = "Enable developer mode")
    private transient boolean devMode = false;

    @Option(name = "--force-console",
            usage = "Never redirect console output to GUI")
    private transient boolean forceConsoleOutput = false;

    @Option(name = "--clear-settings",
            usage = "Clear settings by deleting config.json file, then exit.")
    private transient boolean clearSettings = false;

    @Option(name = "--disable-messages",
            usage = "Disable various info messages (e.g. chest saving).")
    public boolean disableInfoMessages = false;

    @Option(name = "--draw-extended-chunks",
            usage = "Draw extended chunks to map")
    public boolean drawExtendedChunks = false;

    @Option(name = "--enable-cave-mode",
            usage = "Enable automatically switching to cave render mode when underground.")
    public boolean enableCaveRenderMode = false;

    // not really important enough to have an option for, can change it in config file
    public boolean smoothZooming = true;

    // getters
    public static int getExtendedRenderDistance() {
        return instance.extendedRenderDistance;
    }

    public static long getLevelSeed() {
        return instance.levelSeed;
    }

    public static String getWorldOutputDir() {
        return instance.worldOutputDir;
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

    public static boolean renderOtherPlayers() { return instance.renderOtherPlayers; }

    public static VersionReporter versionReporter() {
        return instance.versionReporter;
    }

    public static AuthDetails getManualAuthDetails() {
        return instance.manualAuth;
    }

    public static void setManualAuthDetails(AuthDetails details) {
        instance.manualAuth = details;
    }

    // inverted boolean getters
    public static boolean isWorldGenEnabled() {
        return !instance.disableWorldGen;
    }

    public static boolean markUnsavedChunks() {
        return !instance.disableMarkUnsavedChunks;
    }

    public static boolean handleBlockChanges() {
        return !instance.ignoreBlockChanges;
    }

    public static boolean sendInfoMessages() { return !instance.disableInfoMessages; }

    public static boolean drawExtendedChunks() { return instance.drawExtendedChunks; }

    public static boolean smoothZooming() {
        return instance.smoothZooming;
    }

    public static boolean markOldChunks() {
        return instance.markOldChunks;
    }
    public static boolean enableCaveRenderMode() {
        return instance.enableCaveRenderMode;
    }

    public static MicrosoftAuthHandler getMicrosoftAuth() {
        return instance.microsoftAuth;
    }

    public static void setMicrosoftAuth(MicrosoftAuthHandler microsoftAuth) {
        instance.microsoftAuth = microsoftAuth;
    }

    public static AuthenticationMethod getAuthMethod() {
        return instance.authMethod;
    }

    public static void setAuthMethod(AuthenticationMethod authMethod) {
        instance.authMethod = authMethod;
    }
}

