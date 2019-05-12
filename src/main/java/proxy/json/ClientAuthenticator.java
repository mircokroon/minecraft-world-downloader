package proxy.json;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientAuthenticator {
    private static int STATUS_SUCCESS = 204;
    private static String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/join";

    private LauncherProfiles profiles;

    /**
     * Initialise the authenticator class by reading from the JSON file.
     * Currently only supports to the default location.
     */
    public ClientAuthenticator() {
        Path p = Paths.get(System.getenv("APPDATA"), ".minecraft", "launcher_profiles.json");
        String file = "";
        try {
            file = String.join("\n", Files.readAllLines(p));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot read launcher_profiles.json");
        }

        Gson g = new Gson();
        profiles = g.fromJson(file, LauncherProfiles.class);
    }

    /**
     * Make the authentication request to the Mojang session server. We need to redo this as the one sent by the
     * real client will have had our 'fake' public key instead of the server's real one, and as such the server will
     * not accept the connection.
     * @param hash hash based on the server information.
     */
    public void makeRequest(String hash) throws UnirestException {
        AuthDetails details = profiles.getAuthDetails();

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", details.getAccessToken());
        body.put("selectedProfile", details.getUuid());
        body.put("serverId", hash);


        HttpResponse<String> str = Unirest.post(AUTH_URL)
            .header("Content-Type", "application/json")
            .body(new Gson().toJson(body))
            .asString();

        if (str.getStatus() != STATUS_SUCCESS) {
            throw new RuntimeException("Client not authenticated! " + str.getBody());
        } else {
            System.out.println("Successfully authenticated with Mojang session server.");
        }
    }
}
