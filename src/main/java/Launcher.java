import config.Config;

import util.PathUtils;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static util.ExceptionHandling.attemptQuiet;

public class Launcher {
    public static void main(String[] args) throws URISyntaxException {
        fixCwd();

        Config.init(args);
    }

    private static void fixCwd() throws URISyntaxException {
        String cwd = System.getProperty("user.dir");

        if (Files.isWritable(Paths.get(cwd))) {
            PathUtils.setWorkingDirectory(cwd);
            return;
        }

        // if we can't write to the working directory, try the jar file's location
        Path jarPath = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        if (Files.isWritable(jarPath)) {
            System.out.println("Can't write to working directory. Writing to " + jarPath);
            PathUtils.setWorkingDirectory(jarPath);

            return;
        }

        // if we can't write there, try the Minecraft installation dir
        Path mcPath = Paths.get(Config.getDefaultMinecraftPath(), "world-downloader");
        attemptQuiet(() -> Files.createDirectories(mcPath));
        if (Files.isWritable(mcPath)) {
            System.out.println("Can't write to working directory. Writing to " + mcPath);
            PathUtils.setWorkingDirectory(mcPath);

            return;
        }


        System.err.println("Unable to write data. Consider running with more permissions.");
        System.exit(1);
    }
}
