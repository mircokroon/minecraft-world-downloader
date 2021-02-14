package proxy.auth;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;

/**
 * Deserialization class for the launcher config file.
 */
public class AuthDetails {
    public final static AuthDetails INVALID = new AuthDetails();

    final String uuid;
    final String accessToken;
    final boolean isValid;

    public AuthDetails(String uuid, String accessToken) {
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.isValid = true;
    }

    private AuthDetails() {
        uuid = "";
        accessToken = "";
        isValid = false;
    }

    public static AuthDetails fromUsername(String username, String accessToken) {
        if (username == null || accessToken == null || username.length() == 0 || accessToken.length() == 0) {
            return INVALID;
        }

        if (accessToken.length() != 308) {
            new AuthenticationException("Invalid access token length! Expected 308, found " + accessToken.length())
                    .printStackTrace();

            return INVALID;
        }

        try {
            UuidNameResponse res = ClientAuthenticator.uuidFromUsername(username);
            return new AuthDetails(res.id, accessToken);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return INVALID;
        }
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
