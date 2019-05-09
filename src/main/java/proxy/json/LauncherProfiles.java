package proxy.json;

import java.util.HashMap;

public class LauncherProfiles {
    public HashMap<String, Account> authenticationDatabase;
    public HashMap<String, String> selectedUser;

    public AuthDetails getAuthDetails() {
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
