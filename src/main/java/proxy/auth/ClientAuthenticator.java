package proxy.auth;

import com.google.gson.Gson;
import config.Config;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static util.ExceptionHandling.attempt;
import static util.PrintUtils.devPrint;

public class ClientAuthenticator {
    private static final int STATUS_SUCCESS = 204;
    private static final String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/join";
    private static final String REALMS_URL = "https://pc.realms.minecraft.net/";

    private AuthDetails details;

    private LauncherProfiles profiles;

    private long authDetailsLastModified = 0;

    // for launcher versions 2.2+
    private LauncherAccounts accounts;

    /**
     * Initialise the authenticator class by reading from the JSON file.
     */
    public ClientAuthenticator() { }

    private void retrieveAuthDetails() throws IOException {
        Gson g = new Gson();

        readProfiles(g);
        readAccounts(g);
    }

    private void printAuthErrorMessage() {
        System.err.println("Something went wrong while trying to authenticate your Minecraft account.\n");

        if (Config.inGuiMode()) {
            System.err.println("Set the correct Minecraft installation path in the Connection tab.");
            System.err.println("If you are using a custom launcher, set the correct username and token in the authentication tab.");
        } else {
            System.err.println("Use launch option \"-m /path/to/.minecraft/\" to indicate the location of your Minecraft installation.");
            System.err.println("If you are using a custom launcher, use options --username and --token.");
        }
        System.err.println("See https://github.com/mircokroon/minecraft-world-downloader/wiki/Authentication for more details.");
        System.err.println();
    }

    private void readAccounts(Gson g) throws IOException {
        Path p = Paths.get(Config.getMinecraftPath(), "launcher_accounts.json");

        if (!p.toFile().exists()) {
            // probably not the right version of the launcher
            return;
        }

        authDetailsLastModified = p.toFile().lastModified();

        String path =  String.join("\n", Files.readAllLines(p));

        accounts = g.fromJson(path, LauncherAccounts.class);
    }

    private void readProfiles(Gson g) throws IOException {
        Path p = Paths.get(Config.getMinecraftPath(), "launcher_profiles.json");
        String path =  String.join("\n", Files.readAllLines(p));

        profiles = g.fromJson(path, LauncherProfiles.class);
    }

    /**
     * Get the auth details from the profiles file. If launcher_accounts.json exists, we use that accessToken instead
     * because the other one won't be valid in this case.
     */
    private AuthDetails getAuthDetails() throws IOException {
        if (details != null) {
            return details;
        }

        AuthDetails manualDetails = Config.getManualAuthDetails();

        if (manualDetails != AuthDetails.INVALID) {
            this.details = manualDetails;
            return manualDetails;
        }

        retrieveAuthDetails();

        // Launcher after version 2.2
        if (accounts != null) {
            AuthDetails details = accounts.getAuthDetails();
            if (details != null) {
                this.details = details;
                return details;
            }
        }

        // Launcher before version 2.2
        if (profiles != null) {
            AuthDetails details = profiles.getAuthDetails();
            if (details != null) {
                this.details = details;
                return details;
            }
        }

        throw new AuthenticationException("Cannot find authentication details.");
    }

    /**
     * Make the authentication request to the Mojang session server. We need to do this as the one sent by the
     * real client will have had our 'fake' public key instead of the server's real one, and as such the server will
     * not accept the connection.
     * @param hash hash based on the server information.
     */
    public void makeRequest(String hash) throws UnirestException, AuthenticationException {
        AuthDetails details;
        try {
            details = getAuthDetails();
        } catch (IOException e) {
            printAuthErrorMessage();
            throw new AuthenticationException("Cannot get valid authentication details.", e);
        }

        Map<String, String> body = new HashMap<>();

        body.put("accessToken", details.getAccessToken());
        body.put("selectedProfile", details.getUuid());
        body.put("serverId", hash);


        HttpResponse<String> str = Unirest.post(AUTH_URL)
            .header("Content-Type", "application/json")
            .body(new Gson().toJson(body))
            .asString();

        if (str.getStatus() != STATUS_SUCCESS) {
            if (authDetailsLastModified != 0 && hasProbablyExpired(authDetailsLastModified)) {
                System.err.println("WARNING: authentication details seem to be outdated. " +
                        "Run with --manual-auth if you are using a custom launcher.");
            }
            throw new RuntimeException("Client not authenticated! " + str.getBody());
        } else {
            devPrint("Successfully authenticated user with Mojang session server.");
        }
    }

    public static boolean hasProbablyExpired(Path p) {
        return hasProbablyExpired(p.toFile().lastModified());
    }

    public static boolean hasProbablyExpired(long time) {
        long oneDay = 24 * 60 * 60 * 1000;
        return time + oneDay < System.currentTimeMillis();
    }

    public static AuthStatus authDetailsValid(String path) {
        Path profiles = Paths.get(path, "launcher_profiles.json");
        Path accounts = Paths.get(path, "launcher_accounts.json");

        if (!profiles.toFile().exists()) {
            return new AuthStatus(false, "Cannot find profiles");
        }
        if (!accounts.toFile().exists()) {
            return new AuthStatus(false, "Cannot find accounts");
        }

        AuthStatus status = new AuthStatus(true, "");

        if (hasProbablyExpired(accounts) || hasProbablyExpired(profiles)) {
            status.isProbablyExpired = true;
        }

        return status;
    }

    private void realmRequest(String url, boolean reportErrors, Consumer<String> callback) {
        AuthDetails details;
        try {
            details = getAuthDetails();
        } catch (IOException e) {
            printAuthErrorMessage();
            callback.accept(null);
            return;
        }

        Unirest.get(REALMS_URL + url)
                .cookie("sid", "token:" + details.accessToken + ":" + details.getUuid())
                .cookie("user", details.getUsername())
                .cookie("version", "1.12.2")  // version doesn't seem to actually matter
                .asStringAsync(res -> {
                    if (!res.isSuccess()) {
                        if (reportErrors || (res.getStatus() >= 400 && res.getStatus() < 500)) {
                            new RealmsRequestException(res.getStatus() + " - " + res.getStatusText() + " (body: " + res.getBody() + ")").printStackTrace();
                        }
                        attempt(() -> callback.accept(""));
                    } else {
                        attempt(() -> callback.accept(res.getBody()));
                    }
                });
    }

    public void requestRealmIp(int id, Consumer<String> callback) {
        realmRequest("worlds/v1/" + id + "/join/pc", false ,callback);
    }
    public void requestRealms(Consumer<String> callback) {
        realmRequest("worlds", true, callback);
    }

    public AuthDetails getDetails() {
        return details;
    }
}

interface Prompt {
    String run() throws IOException;
}

class UuidNameResponse {
    String id;
    String name;
}

class RealmsRequestException extends RuntimeException {
    public RealmsRequestException(String message) {
        super("Could not request realms. Reason: " +message);
    }
}
