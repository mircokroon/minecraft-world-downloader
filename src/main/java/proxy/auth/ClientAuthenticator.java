package proxy.auth;

import com.google.gson.Gson;
import game.Game;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientAuthenticator {
    private static int STATUS_SUCCESS = 204;
    private static String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/join";

    private LauncherProfiles profiles;
    private AuthDetails manualDetails;

    // for launcher versions 2.2+
    private LauncherAccounts accounts;

    /**
     * Initialise the authenticator class by reading from the JSON file.
     */
    public ClientAuthenticator() {

        try {
            Gson g = new Gson();


            readProfiles(g);
            readAccounts(g);
        } catch (IOException ex) {
            startAuthDialogue();
        }

    }

    private void readAccounts(Gson g) throws IOException {
        Path p = Paths.get(getMinecraftPath(), "launcher_accounts.json");

        if (!p.toFile().exists()) {
            // probably not the right version of the launcher
            return;
        }

        System.out.println("Reading account information from " + p.toString());

        String path =  String.join("\n", Files.readAllLines(p));

        accounts = g.fromJson(path, LauncherAccounts.class);
    }

    private void readProfiles(Gson g) throws IOException {
        Path p = Paths.get(getMinecraftPath(), "launcher_profiles.json");

        System.out.println("Reading profile information from " + p.toString());

        String path =  String.join("\n", Files.readAllLines(p));

        profiles = g.fromJson(path, LauncherProfiles.class);
    }

    /**
     * Ask the user for their username and access token, not as nice as getting it from the file but it's better than
     * not working at all.
     */
    private void startAuthDialogue() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Cannot read or find launcher_profiles.json!\nYou can use launch option \"-m /path/to/.minecraft/\" to indicate the location of your Minecraft installation.");
        System.out.println("Using manual authentication...");

        String uuid = promptUuid(reader);
        String token = promptAccessToken(reader);
        this.manualDetails = new AuthDetails(uuid, token);
    }

    private static String retry(Prompt r) {
        while (true) {
            try {
                return r.run();
            } catch (IOException ex) { }
        }
    }

    /**
     * Ask the user for their access token, retry if they don't input one that looks correct.
     */
    private String promptAccessToken(BufferedReader reader) {
        System.out.println();
        System.out.println("Your access token is needed for authentication. It...");
        System.out.println("\t- Can be found in launcher_accounts.json, inside your .minecraft directory (for the default launcher)");
        System.out.println("\t- Can be found in launcher_profiles.json, if launcher_accounts.json does not exist");
        System.out.println("\t- Should be named 'accessToken'");
        System.out.println("\t- Is quite long (over 300 characters)");
        System.out.println("\t- Can change when starting the game, so launch Minecraft before entering it");
        System.out.println("If you have multiple accounts, make sure to pick the one matching the given username.");

        return retry(() -> {
            System.out.print("Access token: ");

            // remove trailing spaces and quotes, some people will probably copy those by accident
            String token = reader.readLine().trim().replaceAll("^\"|\"$", "");

            if (token.length() != 308) {
                System.out.println("The given access token is too " + (token.length() > 308 ? "long" : "short") + "!" +
                                       " Make sure you are using the correct token.");
                throw new IOException("Token too short");
            }

            return token;
        });
    }

    /**
     * Ask the user for their username and use it to retrieve the UUID. Retry if the username cannot be found.
     */
    private String promptUuid(BufferedReader reader) {
        Gson g = new Gson();

        return retry(() -> {
            System.out.print("\nMinecraft username: ");
            String username = reader.readLine().trim();

            HttpResponse<String> str = Unirest.get(UUID_URL + username).asString();

            if (!str.isSuccess() || str.getStatus() != 200) {
                System.out.println("Could not get UUID for user '" + username + "'. Status: " + str.getStatus());
                throw new IOException("Cannot find username");
            }

            UuidNameResponse res = g.fromJson(str.getBody(), UuidNameResponse.class);
            System.out.println("Found user '" + res.name + "' with UUID '" + res.id + "'");

            return res.id;
        });
    }

    /**
     * Get the contents of the Minecraft launcher_profiles.json from the given installation path.
     * @return the contents of the file
     */
    private String getMinecraftPath() {
        String path = Game.getGamePath();

        // handle common %APPDATA% env variable for Windows
        if (path.toUpperCase().contains("%APPDATA%") && System.getenv("appdata") != null) {
            String appdataPath = System.getenv("appdata").replace("\\", "\\\\");
            path = path.replaceAll("(?i)%APPDATA%", appdataPath);
        }

        return path;
    }

    /**
     * Get the auth details from the profiles file. If launcher_accounts.json exists, we use that accessToken instead
     * because the other one won't be valid in this case.
     */
    private AuthDetails getAuthDetails() {
        if (profiles != null) {
            AuthDetails details = profiles.getAuthDetails();

            // for launcher version 2.2, check the accounts file for the accessToken instead (why is there 2?)
            if (accounts != null) {
                String token = accounts.getToken();

                if (token != null) {
                    System.out.println("Using accessToken from launcher_accounts.json");
                    details.accessToken = token;
                }
            }
            return details;
        } else {
            return manualDetails;
        }
    }

    /**
     * Make the authentication request to the Mojang session server. We need to do this as the one sent by the
     * real client will have had our 'fake' public key instead of the server's real one, and as such the server will
     * not accept the connection.
     * @param hash hash based on the server information.
     */
    public void makeRequest(String hash) throws UnirestException {
        AuthDetails details = getAuthDetails();

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
            System.out.println("Successfully authenticated user with Mojang session server.");
        }
    }
}

interface Prompt {
    String run() throws IOException;
}

class UuidNameResponse {
    String id;
    String name;
}
