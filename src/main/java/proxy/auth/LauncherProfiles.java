package proxy.auth;

import java.util.HashMap;

/**
 * Deserialization class for the launcher config file.
 */
public class LauncherProfiles {
    public HashMap<String, Account> authenticationDatabase;
    public HashMap<String, String> selectedUser;

    /**
     * Gets the auth details of the currently active profile/user according to the launcher config.
     * For new versions of the launcher, this is no longer in use.
     * @return an object with the relevant authentication details
     */
    public AuthDetails getAuthDetails() {
        if (authenticationDatabase == null || selectedUser == null) {
            return null;
        }

        String accountId = selectedUser.get("account");
        String uuid = selectedUser.get("profile");

        Account account = authenticationDatabase.get(accountId);
        String accessToken = account.accessToken;

        return new AuthDetails(uuid, accessToken);
    }

    @Override
    public String toString() {
        return "LauncherProfiles{" +
            "authenticationDatabase=" + authenticationDatabase +
            ", selectedUser=" + selectedUser +
            '}';
    }
}
