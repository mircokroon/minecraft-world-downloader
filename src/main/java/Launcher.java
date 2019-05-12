import game.Game;
import game.data.WorldManager;
import game.data.chunk.ChunkFactory;
import gui.GuiManager;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Launcher {
    public static void main(String[] args) {
        Game.init(getArguments(args));


        ChunkFactory.startChunkParserService();
        WorldManager.startSaveService();
        GuiManager.showGui();
        Game.startProxy();
    }

    /**
     * Parse commandline arguments.
     */
    private static Namespace getArguments(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Launcher").build()
            .defaultHelp(true)
            .description("Download Minecraft worlds by acting as a proxy and intercepting traffic.");
        parser.addArgument("-s", "--server")
            .setDefault("localhost")
            .help("The address of the remote server to connect to. Hostname or IP address (without port).");
        parser.addArgument("-p", "--port").type(Integer.class)
            .setDefault(25565)
            .help("The port of the remote server.")
         .textualName();
        parser.addArgument("-l", "--local-port").type(Integer.class)
            .setDefault(25565)
            .help("The local port which the client has to connect to.")
            .dest("local-port");
        parser.addArgument("-o", "--output")
            .setDefault("world")
            .help("The world output directory. If the world already exists, it will attempt to merge with it.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch(ArgumentParserException ex) {
            parser.handleError(ex);
            System.exit(1);
        }
        return ns;
    }
}
