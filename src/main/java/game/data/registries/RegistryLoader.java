package game.data.registries;

import gui.GuiManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import config.Config;
import game.UnsupportedMinecraftVersionException;
import game.data.chunk.BlockEntityRegistry;
import game.data.chunk.palette.GlobalPalette;
import game.data.container.ItemRegistry;
import game.data.container.MenuRegistry;
import game.data.entity.EntityNames;
import game.data.villagers.VillagerProfessionRegistry;
import game.data.villagers.VillagerTypeRegistry;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import util.PathUtils;

/**
 * Download the relevant server.jar file and generate the reports, including entity IDs and block IDs.
 */
public class RegistryLoader {
    private static final String CACHE = "cache";
    private static final String OUTPUT = "generated";
    private static final String REPORTS = "reports";
    private static final String REGISTRY_FILENAME = "registries.json";
    private static final String BLOCKS_FILENAME = "blocks.json";

    private final Path serverPath, registriesGeneratedPath, blocksGeneratedPath;
    private final Path destinationPath, registryPath, blocksPath;

    private final String version;

    private static final Map<String, RegistryLoader> knownLoaders = new ConcurrentHashMap<>();

    public static RegistryLoader forVersion(String version) {
        return knownLoaders.computeIfAbsent(version, (v) -> {
            try {
                return new RegistryLoader(v);
            } catch (IOException|InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private RegistryLoader(String version) throws IOException, InterruptedException {
        serverPath = PathUtils.toPath(CACHE, "server.jar");
        registriesGeneratedPath = PathUtils.toPath(CACHE, OUTPUT, REPORTS, REGISTRY_FILENAME);
        blocksGeneratedPath = PathUtils.toPath(CACHE, OUTPUT, REPORTS, BLOCKS_FILENAME);

        this.version = version;
        String versionPath = version.replaceAll("\\.", "_");

        this.destinationPath = PathUtils.toPath(CACHE, versionPath);
        this.blocksPath = PathUtils.toPath(CACHE, versionPath, BLOCKS_FILENAME);
        this.registryPath = PathUtils.toPath(CACHE, versionPath, REGISTRY_FILENAME);

        if (!hasExistingReports()) {
            getReportsFromServerJar();
        }
    }

    /**
     * Checks if json files already exist containing the reports for this version. 1.12.2 has separate handling as the
     * server jar couldn't yet generate reports at this point.
     */
    private boolean hasExistingReports() {
        if (version.equals("1.13.2")) {
            System.out.println("No item registry for 1.13.2. We won't be able to save chests contents :(");
        }

        return version.equals("1.12.2") || blocksPath.toFile().exists();
    }

    /**
     * If we don't have the report, we'll have to download the relevant server.jar and generate them. We'll print some
     * helpful messages as well to put the user at ease about the delay.
     */
    private void getReportsFromServerJar() throws IOException, InterruptedException {
        GuiManager.setStatusMessage("Running version " + version + " for the first time." +
                                        " Generating reports... (this may take a few minutes)");

        System.out.println("Generating reports for version " + version + ".");

        String serverUrl = VersionManifestHandler.findServerUrl(version);

        downloadServerJar(serverUrl);
        generateReports();
        moveReports();
        clean();

        GuiManager.setStatusMessage("");
    }

    /**
     * Download the correct server.jar for this version.
     * @param url the url, cannot really be guessed so these are read in from a file.
     */
    private void downloadServerJar(String url) throws IOException {
        System.out.println("Downloading this version's server.jar (" + url + ")");
        HttpResponse<byte[]> status = Unirest.get(url)
            .asBytes();

        Files.createDirectories(PathUtils.toPath(CACHE));

        // in case we can't download the server.jar
        if (!status.isSuccess()) {
            throw new IOException("Unable to download server.jar. Status: " + status.getStatusText());
        }

        Files.write(serverPath, status.getBody());
    }

    /**
     * Generate the reports using the server.jar, it's a bit slow. We want to know if something goes wrong so we
     * redirect output from the server.jar process to the console.
     */
    private void generateReports() throws IOException, InterruptedException {
        System.out.println("We'll generate some reports now, this may take a minute.");
        System.out.println("Starting output of Minecraft server.jar:");

        ProcessBuilder pb;
        if (Config.versionReporter().isAtLeast1_18()) {
            pb = new ProcessBuilder(
                    "java", "-DbundlerMainClass=net.minecraft.data.Main", "-jar", "server.jar", "--reports"
            );
        } else {
            pb = new ProcessBuilder(
                    "java", "-cp", "server.jar", "net.minecraft.data.Main", "--reports"
            );
        }

        pb.directory(PathUtils.toPath(CACHE).toFile());
        Process p = pb.start();

        // instead of directly forwarding the output, we handle it manually. This way we can indent it and get rid
        // of the annoying teleport command spam.
        printStream(p.getInputStream());
        printStream(p.getErrorStream());

        p.waitFor();

        System.out.println("Completed generating reports!");
    }

    private void printStream(InputStream str) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(str));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Ambiguity between arguments")) {
                continue;
            }
            System.out.println("\t" + line);
        }
    }

    /**
     * Move newly generated reports to the directory where we expect to find them later.
     */
    private void moveReports() throws IOException {
        Files.createDirectories(destinationPath);

        if (versionSupportsGenerators() && Files.exists(registriesGeneratedPath)) {
            Files.move(registriesGeneratedPath, registryPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (versionSupportsBlockGenerator() && Files.exists(blocksGeneratedPath)) {
            Files.move(blocksGeneratedPath, blocksPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Delete the server.jar and all the files it generated.
     */
    public void clean() throws IOException {
        FileUtils.deleteDirectory(PathUtils.toPath(CACHE, OUTPUT).toFile());
        FileUtils.deleteDirectory(PathUtils.toPath(CACHE, "logs").toFile());
        FileUtils.deleteDirectory(PathUtils.toPath(CACHE, "logsx").toFile());
        Files.deleteIfExists(serverPath);
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

    public BlockEntityRegistry generateBlockEntityRegistry() throws IOException {
        if (versionSupportsGenerators()) {
            return BlockEntityRegistry.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else {
            return new BlockEntityRegistry();
        }
    }

    public VillagerProfessionRegistry generateVillagerProfessionRegistry() throws FileNotFoundException {
        if (versionSupportsGenerators()) {
            return VillagerProfessionRegistry.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else {
            return new VillagerProfessionRegistry();
        }
    }

    public VillagerTypeRegistry generateVillagerTypeRegistry() throws FileNotFoundException {
        if (versionSupportsGenerators()) {
            return VillagerTypeRegistry.fromRegistry(new FileInputStream(registryPath.toFile()));
        } else {
            return new VillagerTypeRegistry();
        }
    }

    public boolean versionSupportsBlockGenerator() {
        return !version.startsWith("1.12");
    }

    public boolean versionSupportsGenerators() {
        return versionSupportsBlockGenerator() && !version.startsWith("1.13");
    }
}
