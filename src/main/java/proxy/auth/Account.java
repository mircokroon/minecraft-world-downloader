package proxy.auth;

import java.util.HashMap;

/**
 * Deserialization class for the launcher config file.
 */
public class Account {
    public String accessToken;
    public String username;
    public HashMap<String, Profile> profiles;

    @Override
    public String toString() {
        return "Account{" +
            "accessToken='" + accessToken + '\'' +
            ", username='" + username + '\'' +
            ", profiles=" + profiles +
            '}';
    }
}
