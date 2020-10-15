package game.data.registries;

import com.google.gson.Gson;

import game.UnsupportedMinecraftVersionException;
import game.data.chunk.entity.EntityNames;
import game.data.chunk.palette.GlobalPalette;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

/**
 * Download the relevant server.jar file and generate the reports, including entity IDs and block IDs.
 */
public class RegistryLoader {
    private static final String CACHE = "cache";
    private static final String OUTPUT = "generated";
    private static final String REPORTS = "reports";
    private static final String REGISTRY_FILENAME = "registries.json";
    private static final String BLOCKS_FILENAME = "blocks.json";

    private static final Path SERVER_PATH = Paths.get(CACHE, "server.jar");
    private static final Path REGISTRIES_GENERATED_PATH = Paths.get(CACHE, OUTPUT, REPORTS, REGISTRY_FILENAME);
    private static final Path BLOCKS_GENERATED_PATH = Paths.get(CACHE, OUTPUT, REPORTS, BLOCKS_FILENAME);

    private String version;
    private Path destinationPath;
    private Path registryPath;
    private Path blocksPath;

    public RegistryLoader(String version) throws IOException, InterruptedException {
        this.version = version;
        String versionPath = version.replaceAll("\\.", "_");

        this.destinationPath = Paths.get(CACHE, versionPath);
        this.blocksPath = Paths.get(CACHE, versionPath, BLOCKS_FILENAME);
        this.registryPath = Paths.get(CACHE, versionPath, REGISTRY_FILENAME);

        if (!hasExistingReports()) {
            getReportsFromServerJar();
        }
    }

    /**
     * Checks if json files already exist containing the reports for this version. 1.12.2 has separate handling as the
     * server jar couldn't yet generate reports at this point.
     */
    private boolean hasExistingReports() {
        return version.equals("1.12.2") || blocksPath.toFile().exists() && registryPath.toFile().exists();
    }

    /**
     * If we don't have the report, we'll have to download the relevant server.jar and generate them. We'll print some
     * helpful messages as well to put the user at ease about the delay.
     */
    private void getReportsFromServerJar() throws IOException, InterruptedException {
        System.out.println("Looks like we have not run in version " + version + " before.");
        String file = "server.json";
        InputStream input = EntityNames.class.getClassLoader().getResourceAsStream(file);

        if (input == null) {
            throw new FileNotFoundException("Version.json not found");
        }

        VersionMap map = new Gson().fromJson(new InputStreamReader(input), VersionMap.class);

        if (!map.containsKey(version)) {
            throw new IllegalArgumentException("Cannot find given version: " + version + ". Are you using the latest world downloader version?");
        }

        downloadServerJar(map.get(version));
        generateReports();
        moveReports();
        clean();
    }

    /**
     * Download the correct server.jar for this version.
     * @param url the url, cannot really be guessed so these are read in from a file.
     */
    private void downloadServerJar(String url) throws IOException {
        System.out.println("Downloading this version's server.jar (" + url + ")");
        HttpResponse<byte[]> status = Unirest.get(url)
            .asBytes();

        ensureExists(Paths.get(CACHE));
        Files.write(SERVER_PATH, status.getBody());
    }

    /**
     * Generate the reports using the server.jar, it's a bit slow. We want to know if something goes wrong so we
     * redirect output from the server.jar process to the console.
     */
    private void generateReports() throws IOException, InterruptedException {
        System.out.println("We'll generate some reports now, this may take a minute.");
        System.out.println("Starting output of Minecraft server.jar:");
        System.out.println("=\t=\t=\t=\t=\t=\t=\t=");

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", "server.jar", "net.minecraft.data.Main", "--reports"
        ).inheritIO();
        pb.directory(new File(CACHE));
        Process p = pb.start();
        p.waitFor();

        System.out.println("=\t=\t=\t=\t=\t=\t=\t=");
        System.out.println("Completed generating reports!");
    }

    /**
     * Move newly generated reports to the directory where we expect to find them later.
     */
    private void moveReports() throws IOException {
        ensureExists(destinationPath);

        if (versionSupportsGenerators()) {
            Files.move(REGISTRIES_GENERATED_PATH, registryPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (versionSupportsBlockGenerator()) {
            Files.move(BLOCKS_GENERATED_PATH, blocksPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Delete the server.jar and all the files it generated.
     */
    public void clean() throws IOException {
        FileUtils.deleteDirectory(Paths.get(CACHE, OUTPUT).toFile());
        FileUtils.deleteDirectory(Paths.get(CACHE, "logs").toFile());
        FileUtils.deleteDirectory(Paths.get(CACHE, "logsx").toFile());
        Files.deleteIfExists(SERVER_PATH);
    }

    /**
     * Make sure the given folder exists.
     */
    private void ensureExists(Path folder) {
        File dir = folder.toFile();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    public EntityNames generateEntityNames() throws IOException {
        if (versionSupportsGenerators()) {
            return EntityNames.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else if (version.equals("1.12.2")) {
            return EntityNames.fromJson("1.12.2");
        } else if (version.equals("1.13.2")) {
            return EntityNames.fromJson("1.13.2");
        } else {
            throw new UnsupportedMinecraftVersionException(version);
        }
    }

    public GlobalPalette generateGlobalPalette() throws IOException {
        if (versionSupportsBlockGenerator()) {
            return new GlobalPalette(new FileInputStream(blocksPath.toFile()));
        } else {
            return new GlobalPalette("1.12.2");
        }
    }

    public MenuRegistry generateMenuRegistry() throws IOException {
        if (versionSupportsGenerators()) {
            return MenuRegistry.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else {
            return new MenuRegistry();
        }
    }

    public ItemRegistry generateItemRegistry() throws IOException {
        if (versionSupportsGenerators()) {
            return ItemRegistry.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else if (version.equals("1.12.2")) {
            return ItemRegistry.fromJson(version);
        } else {
            return new ItemRegistry();
        }
    }

    public boolean versionSupportsBlockGenerator() {
        return !version.startsWith("1.12");
    }

    public boolean versionSupportsGenerators() {
        return versionSupportsBlockGenerator() && !version.startsWith("1.13");
    }
}

class VersionMap extends HashMap<String, String> { }
