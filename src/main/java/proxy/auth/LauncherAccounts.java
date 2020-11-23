package proxy.auth;

import java.util.HashMap;

/**
 * Corresponds to launcher_accounts.json from launcher version 2.2+. This file will hold the actually valid accessToken
 * instead of the launcher_profiles.json in this case.
 */
public class LauncherAccounts {
    HashMap<String, LauncherAccount> accounts;
    String activeAccountLocalId;

    String getToken() {
        if (accounts == null) { return null; }

        LauncherAccount account = accounts.get(activeAccountLocalId);

        if (account == null) { return null; }

        return account.accessToken;
    }

    @Override
    public String toString() {
        return "LauncherAccounts{" +
            "accounts=" + accounts +
            '}';
    }
}

class LauncherAccount {
    String accessToken;

    @Override
    public String toString() {
        return "LauncherAccount{" +
            "accessToken='" + accessToken + '\'' +
            '}';
    }
}
