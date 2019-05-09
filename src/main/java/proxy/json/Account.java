package proxy.json;

import java.util.HashMap;

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
