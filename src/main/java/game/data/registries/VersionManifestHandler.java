package game.data.registries;

import com.google.gson.Gson;
import game.UnsupportedMinecraftVersionException;
import kong.unirest.Unirest;

import java.util.HashMap;
import java.util.List;

/**
 * Class to handle loading data from the Mojang version manifest. 
 */
public class VersionManifestHandler {
    private static final String manifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public static String findServerUrl(String desiredVersion) {
        Gson g = new Gson();

        String manifestResponse = Unirest.get(manifestUrl).asString().getBody();
        JsonManifest manifest = g.fromJson(manifestResponse, JsonManifest.class);

        String versionUrl = manifest.findUrl(desiredVersion);
        if (versionUrl == null) {
            throw new UnsupportedMinecraftVersionException("Cannot find version " + desiredVersion + " in version manifest");
        }

        String versionResponse = Unirest.get(versionUrl).asString().getBody();
        String finalUrl = g.fromJson(versionResponse, JsonSpecificVersion.class).getServerUrl();
        if (finalUrl == null || finalUrl.length() < 16) {
            throw new UnsupportedMinecraftVersionException("No valid URL found for version " + desiredVersion);
        }

        return finalUrl;
    }

    class JsonManifest {
        List<JsonManifestVersion> versions;

        String findUrl(String version) {
            for (JsonManifestVersion jmv : versions) {
                if (jmv.id.equals(version)) {
                    return jmv.url;
                }
            }
            return null;
        }
        class JsonManifestVersion {
            String id;
            String url;
        }
    }

    class JsonSpecificVersion {
        HashMap<String, JsonDownload> downloads;

        String getServerUrl() {
            return downloads.get("server").url;
        }

        class JsonDownload {
            String url;
        }
    }
}




