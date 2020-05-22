package game.data;

import com.google.gson.Gson;

import game.data.chunk.entity.EntityNames;
import game.data.chunk.palette.GlobalPalette;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Download the relevant server.jar file and generate the reports.
 */
public class RegistryLoader {
    private static final Path SERVER_PATH = Paths.get("tmp/server.jar");
    private static final Path REPORTS_PATH = Paths.get("tmp/generated/reports/");
    private static final Path REGISTRY_PATH = Paths.get("tmp/generated/reports/registries.json");
    private static final Path BLOCKS_PATH = Paths.get("tmp/generated/reports/blocks.json");

    public static void main(String[] args) throws IOException, InterruptedException {
        new RegistryLoader("1.15.2").generateEntityNames();
    }

    public EntityNames generateEntityNames() throws IOException {
        return EntityNames.fromRegistry(new FileInputStream(REGISTRY_PATH.toFile()));
    }

    public GlobalPalette generateGlobalPalette() throws IOException {
        return new GlobalPalette(new FileInputStream(BLOCKS_PATH.toFile()));
    }

    public void clean() throws IOException {
        Files.deleteIfExists(SERVER_PATH);
        // Files.deleteIfExists(REPORTS_PATH);
    }

    public RegistryLoader(String version) throws IOException, InterruptedException {
        String file = "server.json";
        InputStream input = EntityNames.class.getClassLoader().getResourceAsStream(file);

        if (input == null) { throw new FileNotFoundException("Version.json not found"); }

        VersionMap map = new Gson().fromJson(new InputStreamReader(input), VersionMap.class);

        if (!map.containsKey(version)) {
            throw new IllegalArgumentException("Cannot find given version: " + version);
        }
        Files.createDirectories(Paths.get("tmp"));
        downloadServerJar(map.get(version));
        generateReports();
    }

    private void generateReports() throws IOException, InterruptedException {
        System.out.println("Starting output of Minecraft server.jar:");
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", "server.jar", "net.minecraft.data.Main", "--reports"
        ).inheritIO();
        pb.directory(new File("tmp"));
        Process p = pb.start();
        p.waitFor();
        System.out.println("Completed generating reports.");
    }

    private void downloadServerJar(String url) throws IOException {
        System.out.println("Downloading " + url);
        HttpResponse<byte[]> status = Unirest.get(url)
            .asBytes();

        System.out.println(status.getStatus());
        System.out.println(status.getStatusText());

        Files.write(SERVER_PATH, status.getBody());
    }
}

class VersionMap extends HashMap<String, String> { }
