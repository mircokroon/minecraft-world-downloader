package proxy.json;

/**
 * Deserialization class for the launcher config file.
 */
public class AuthDetails {
    String uuid;
    String accessToken;

    public AuthDetails(String uuid, String accessToken) {
        this.uuid = uuid;
        this.accessToken = accessToken;
    }

    public String getUuid() {
        return uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "AuthDetails{" +
            "uuid='" + uuid + '\'' +
            ", accessToken='" + accessToken + '\'' +
            '}';
    }
}
