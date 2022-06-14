package proxy.auth;

import config.Config;

import gui.GuiManager;
import java.io.IOException;

/**
 * Class to handle retrieving authentication details from either user input, or the relevant launcher files.
 */
public abstract class AuthDetailsManager {
    private AuthDetails details;
    private String username;

    public AuthDetailsManager() { }

    public AuthDetailsManager(String username) {
        this.username = username;
    }

    private static AuthDetails retrieveDetailsFromProcess() throws IOException {
        return new AuthDetailsFromProcess().getDetails();
    }

    private static AuthDetails retrieveDetailsFromMicrosoft() {
        MicrosoftAuthHandler msAuth = Config.getMicrosoftAuth();
        if (msAuth == null) {
            return AuthDetails.INVALID;
        }
        return msAuth.getAuthDetails();
    }

    /**
     * Get the auth details from the profiles file. If launcher_accounts.json exists, we use that accessToken instead
     * because the other one won't be valid in this case.
     */
    public static AuthDetails loadAuthDetails() throws IOException {
        GuiManager.setStatusMessage("Getting Minecraft authentication details...");

        return switch (Config.getAuthMethod()) {
            case AUTOMATIC -> retrieveDetailsFromProcess();
            case MICROSOFT -> retrieveDetailsFromMicrosoft();
            case MANUAL -> Config.getManualAuthDetails();
        };
    }

    public AuthDetails getAuthDetails() throws IOException {
        if (this.details == null) {
            this.details = loadAuthDetails();
        }
        return this.details;
    }

    public void reset() {
        this.details = null;
    }

    protected void printAuthErrorMessage() {
        System.err.println("Something went wrong while trying to authenticate your Minecraft account.\n");

        if (Config.inGuiMode()) {
            System.err.println("Authentication details are retrieved from the Minecraft game directly. Make sure that the game is running.");
            System.err.println("If you have a username set in the authentication tab, ensure that it matches the one you are trying to connect with.");
            System.err.println("If you are in an environment where this does not work, please set the correct username and token in the authentication tab.");
        } else {
            System.err.println("Authentication details are retrieved from the Minecraft process's launch arguments. The game must be running for this to work.");
            System.err.println("Alternatively, use options --username and --token to specify the details manually.");
        }

        System.err.println("See https://github.com/mircokroon/minecraft-world-downloader/wiki/Authentication for more details.");
        System.err.println();
    }
}