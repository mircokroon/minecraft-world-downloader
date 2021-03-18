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

/**
 * Class to handle retrieving authentication details from either user input, or the relevant launcher files.
 */
public abstract class AuthDetailsManager {
    private AuthDetails details;

    private LauncherProfiles profiles;

    private long authDetailsLastModified = 0;

    // for launcher versions 2.2+
    private LauncherAccounts accounts;

    /**
     * Initialise the authenticator class by reading from the JSON file.
     */
    public AuthDetailsManager() { }

    private void retrieveAuthDetails() throws IOException {
        Gson g = new Gson();

        readProfiles(g);
        readAccounts(g);
    }

    protected void printAuthErrorMessage() {
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
    protected AuthDetails getAuthDetails() throws IOException {
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

    public AuthDetails getDetails() {
        return details;
    }

    protected long getDetailsLastModified() {
        return authDetailsLastModified;
    }
}