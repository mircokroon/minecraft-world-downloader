package game.data;

import com.google.gson.Gson;

import game.data.chunk.entity.EntityNames;
import game.data.chunk.palette.GlobalPalette;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

/**
 * Download the relevant server.jar file and generate the reports.
 */
public class RegistryLoader {
    private static final String CACHE = "cache";
    private static final String OUTPUT = "generated";
    private static final String REPORTS = "reports";
    private static final String REGISTRY_FILENAME =  "registries.json";
    private static final String BLOCKS_FILENAME = "blocks.json";

    private static final Path SERVER_PATH = Paths.get(CACHE, "server.jar");
    private static final Path REGISTRIES_GENERATED_PATH = Paths.get(CACHE, OUTPUT, REPORTS, REGISTRY_FILENAME);
    private static final Path BLOCKS_GENERATED_PATH = Paths.get(CACHE, OUTPUT, REPORTS, BLOCKS_FILENAME);

    private String version;
    private Path destinationPath;
    private Path registryPath;
    private Path blocksPath;

    public static void main(String[] args) throws IOException, InterruptedException {
        new RegistryLoader("1.15.2").generateEntityNames();
    }

    public EntityNames generateEntityNames() throws IOException {
        return EntityNames.fromRegistry(new FileInputStream(registryPath.toFile()));
    }

    public GlobalPalette generateGlobalPalette() throws IOException {
        return new GlobalPalette(new FileInputStream(blocksPath.toFile()));
    }

    public void clean() throws IOException {
        FileUtils.deleteDirectory(Paths.get(CACHE, OUTPUT).toFile());
        FileUtils.deleteDirectory(Paths.get(CACHE, "logs").toFile());
        FileUtils.deleteDirectory(Paths.get(CACHE, "logsx").toFile());
        Files.deleteIfExists(SERVER_PATH);
    }

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

    private boolean hasExistingReports() {
        return blocksPath.toFile().exists() && registryPath.toFile().exists();
    }

    private void getReportsFromServerJar() throws IOException, InterruptedException {
        System.out.println("Looks like we have not run in version " + version + " before.");
        String file = "server.json";
        InputStream input = EntityNames.class.getClassLoader().getResourceAsStream(file);

        if (input == null) { throw new FileNotFoundException("Version.json not found"); }

        VersionMap map = new Gson().fromJson(new InputStreamReader(input), VersionMap.class);

        if (!map.containsKey(version)) {
            throw new IllegalArgumentException("Cannot find given version: " + version);
        }

        downloadServerJar(map.get(version));
        generateReports();
        moveReports();
        clean();
    }

    private void generateReports() throws IOException, InterruptedException {
        System.out.println("We'll generate some reports now, this may take a minute.");
        System.out.println("Starting output of Minecraft server.jar:");
        System.out.println("=\t=\t=\t=");

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", "server.jar", "net.minecraft.data.Main", "--reports"
        ).inheritIO();
        pb.directory(new File(CACHE));
        Process p = pb.start();
        p.waitFor();

        System.out.println("=\t=\t=\t=");
        System.out.println("Completed generating reports!");
    }

    private void ensureExists(Path folder) {
        File dir = folder.toFile();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    private void moveReports() throws IOException {
        ensureExists(destinationPath);
        Files.move(REGISTRIES_GENERATED_PATH, registryPath, StandardCopyOption.REPLACE_EXISTING);
        Files.move(BLOCKS_GENERATED_PATH, blocksPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void downloadServerJar(String url) throws IOException {
        System.out.println("Downloading this version's server.jar (" + url + ")");
        HttpResponse<byte[]> status = Unirest.get(url)
            .asBytes();

        ensureExists(Paths.get(CACHE));
        Files.write(SERVER_PATH, status.getBody());
    }
}

class VersionMap extends HashMap<String, String> { }
