package game.data.registries;

import config.Version;
import gui.GuiManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import config.Config;
import game.UnsupportedMinecraftVersionException;
import game.data.chunk.BlockEntityRegistry;
import game.data.chunk.palette.BlockRegistry;
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

        String javaRuntime = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        if (runServerDataGenerator(javaRuntime)) {
            System.out.println("Completed generating reports!");
            return;
        }

        // the server.jar for newer Minecraft versions can require a newer Java runtime than the one this
        // application itself is running on. Look for a newer JDK installed alongside the current one (e.g. in
        // IntelliJ's ~/.jdks) and retry with that before giving up.
        String newerJava = findNewerJavaExecutable();
        if (newerJava == null) {
            throw new IOException(
                "Version " + version + "'s server.jar needs a newer Java runtime than the one running this "
                    + "application (" + javaRuntime + "). Install a newer JDK and try again."
            );
        }

        System.out.println("The Java runtime running this application is too old for this version's server.jar.");
        System.out.println("Retrying with: " + newerJava);

        if (!runServerDataGenerator(newerJava)) {
            throw new IOException("Could not generate reports for version " + version + " even with " + newerJava + ".");
        }

        System.out.println("Completed generating reports!");
    }

    /**
     * Run the server.jar's report generator with the given java executable.
     * @return false if it failed because the runtime is too old to load the server.jar, so the caller can retry
     *         with a newer one.
     */
    private boolean runServerDataGenerator(String javaExecutable) throws IOException, InterruptedException {
        ProcessBuilder pb;
        if (Config.versionReporter().isAtLeast(Version.V1_18)) {
            pb = new ProcessBuilder(
                javaExecutable, "-DbundlerMainClass=net.minecraft.data.Main", "-jar", "server.jar", "--reports"
            );
        } else {
            pb = new ProcessBuilder(
                javaExecutable, "-cp", "server.jar", "net.minecraft.data.Main", "--reports"
            );
        }

        pb.directory(PathUtils.toPath(CACHE).toFile());
        Process p = pb.start();

        // instead of directly forwarding the output, we handle it manually. This way we can indent it and get rid
        // of the annoying teleport command spam.
        boolean tooOld = printStream(p.getInputStream());
        tooOld |= printStream(p.getErrorStream());

        p.waitFor();

        return !tooOld;
    }

    /**
     * Look for a JDK installed next to the one currently running this application (e.g. IntelliJ installs its
     * managed JDKs side by side in ~/.jdks on every OS) and return the java executable of the newest one found.
     */
    private String findNewerJavaExecutable() {
        Path currentJdk = Paths.get(System.getProperty("java.home"));
        Path candidateRoot = currentJdk.getParent();
        if (candidateRoot == null || !Files.isDirectory(candidateRoot)) {
            return null;
        }

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String best = null;
        int bestVersion = -1;

        try (Stream<Path> dirs = Files.list(candidateRoot)) {
            for (Path dir : (Iterable<Path>) dirs::iterator) {
                Path javaBin = dir.resolve("bin").resolve(isWindows ? "java.exe" : "java");
                if (!Files.isRegularFile(javaBin)) {
                    continue;
                }

                int version = getJavaMajorVersion(javaBin);
                if (version > bestVersion) {
                    bestVersion = version;
                    best = javaBin.toString();
                }
            }
        } catch (IOException e) {
            return null;
        }

        return best;
    }

    private int getJavaMajorVersion(Path javaBin) {
        try {
            Process p = new ProcessBuilder(javaBin.toString(), "-version").redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            p.waitFor();

            Matcher m = Pattern.compile("version \"(\\d+)(?:\\.(\\d+))?").matcher(output);
            if (m.find()) {
                int major = Integer.parseInt(m.group(1));
                // old "1.8" style version numbers
                return major == 1 ? Integer.parseInt(m.group(2)) : major;
            }
        } catch (IOException | InterruptedException | NumberFormatException e) {
            // not a usable java executable, ignore it
        }
        return -1;
    }

    private boolean printStream(InputStream str) throws IOException {
        boolean tooOld = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(str));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Ambiguity between arguments")) {
                continue;
            }
            if (line.contains("UnsupportedClassVersionError")) {
                tooOld = true;
            }
            System.out.println("\t" + line);
        }
        return tooOld;
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

    public BlockRegistry generateGlobalPalette() throws IOException {
        if (versionSupportsBlockGenerator()) {
            return new BlockRegistry(new FileInputStream(blocksPath.toFile()));
        } else {
            return new BlockRegistry("1.12.2");
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
