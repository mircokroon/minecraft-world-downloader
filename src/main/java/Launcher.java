import game.Config;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.SystemUtils;
import proxy.ConnectionManager;

import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args) {
        Config.init(getArguments(args));

        new ConnectionManager().startProxy();
    }

    /**
     * Parse commandline arguments.
     */
    private static Namespace getArguments(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("world-downloader.jar").build()
                .defaultHelp(true)
                .description("Download Minecraft worlds by reading chunk data from network traffic.");
        parser.addArgument("-s", "--server")
                .required(true)
                .help("The address of the remote server to connect to. Hostname or IP address (without port).");
        parser.addArgument("-p", "--port").type(Integer.class)
                .setDefault(25565)
                .help("The port of the remote server.");
        parser.addArgument("-l", "--local-port").type(Integer.class)
                .setDefault(25565)
                .help("The local port which the client has to connect to.")
                .dest("local-port");
        parser.addArgument("-o", "--output")
                .setDefault("world")
                .help("The world output directory. If the world already exists, it will attempt to merge with it.");
        parser.addArgument("-r", "--extended-render-distance").dest("extended-distance")
                .setDefault(0)
                .type(Integer.class)
                .help("When set, send downloaded chunks to client to extend render distance to given amount.");
        parser.addArgument("-b", "--mask-bedrock").dest("mask-bedrock")
                .setDefault(false)
                .type(boolean.class)
                .help("Convert all bedrock to stone to make world locations harder to find. Currently only for 1.12.2.");
        parser.addArgument("--center-x")
                .setDefault(0)
                .type(Integer.class)
                .dest("center-x")
                .help("Center for x-coordinate. World will be offset by this coordinate so that its centered around 0.");
        parser.addArgument("--center-z")
                .setDefault(0)
                .type(Integer.class)
                .dest("center-z")
                .help("Center for z-coordinate. World will be offset by this coordinate so that its centered around 0.");
        parser.addArgument("-g", "--gui")
                .setDefault(true)
                .type(boolean.class)
                .help("Show GUI indicating which chunks have been saved.");
        parser.addArgument("-e", "--seed")
                .setDefault(0L)
                .type(Long.class)
                .help("Level seed for output file, as a long.");
        parser.addArgument("-m", "--minecraft")
                .setDefault(getDefaultPath())
                .help("Path to your Minecraft installation, used to authenticate with Mojang servers.");
        parser.addArgument("-z", "--overview-zoom").dest("overview-zoom")
                .setDefault(75)
                .type(Integer.class)
                .help("Render distance of (in chunks) of the overview map.");
        parser.addArgument("--mark-new-chunks").dest("mark-new-chunks")
                .setDefault(false)
                .type(boolean.class)
                .help("Mark new chunks in an orange outline.");
        parser.addArgument("--write-chunks").dest("write-chunks")
                .setDefault(true)
                .type(boolean.class)
                .help("Set to false to disable writing the chunks, mostly for debugging purposes.");
        parser.addArgument("-w", "--enable-world-gen").dest("enable-world-gen")
                .setDefault(true)
                .type(boolean.class)
                .help("When false, set world type to a superflat void to prevent new chunks from being added.");
        parser.addArgument("--enable-srv-lookup").dest("enable-srv-lookup")
                .setDefault(true)
                .type(boolean.class)
                .help("When true, checks for true address using DNS service records");
        parser.addArgument("-u", "--mark-unsaved-chunks").dest("mark-unsaved-chunks")
                .setDefault(true)
                .type(boolean.class)
                .help("When true, marks chunks red in the overview until they are written to the disk.");
        parser.addArgument("--dev-mode").dest("dev-mode")
                .setDefault(false)
                .type(Boolean.class)
                .help("Enables developer mode.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException ex) {
            parser.handleError(ex);
            System.exit(1);
        }
        return ns;
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
}
