package proxy.auth;

import java.util.HashMap;

/**
 * Corresponds to launcher_accounts.json from launcher version 2.2+. This file will hold the actually valid accessToken
 * instead of the launcher_profiles.json in this case.
 */
public class LauncherAccounts {
    HashMap<String, LauncherAccount> accounts;
    String activeAccountLocalId;

    @Override
    public String toString() {
        return "LauncherAccounts{" +
            "accounts=" + accounts +
            '}';
    }

    public AuthDetails getAuthDetails() {
        if (accounts == null || !accounts.containsKey(activeAccountLocalId)) { return null; }
        LauncherAccount account = accounts.get(activeAccountLocalId);

        return new AuthDetails(account.minecraftProfile.id, account.accessToken);
    }
}

class LauncherAccount {
    String accessToken;
    MinecraftProfile minecraftProfile;

    @Override
    public String toString() {
        return "LauncherAccount{" +
                "accessToken='" + accessToken + '\'' +
                ", profile=" + minecraftProfile +
                '}';
    }
}

class MinecraftProfile {
    String id;
    String name;

    @Override
    public String toString() {
        return "MinecraftProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
