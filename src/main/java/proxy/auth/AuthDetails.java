package proxy.auth;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;

/**
 * Deserialization class for the launcher config file.
 */
public class AuthDetails {
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    public final static AuthDetails INVALID = new AuthDetails();

    final String uuid;
    final String accessToken;
    final String name;
    final boolean isValid;

    public AuthDetails(String name, String uuid, String accessToken) {
        this.name = name;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.isValid = true;
    }

    private AuthDetails() {
        name = "";
        uuid = "";
        accessToken = "";
        isValid = false;
    }

    /**
     * Load auth details from a username and accesstoken. User UUID is acquired from Mojang API.
     */
    public static AuthDetails fromUsername(String username, String accessToken) {
        if (username == null || accessToken == null || username.length() == 0 || accessToken.length() == 0) {
            return INVALID;
        }

        try {
            UuidNameResponse res = uuidFromUsername(username);
            return new AuthDetails(username, res.id, accessToken);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return INVALID;
        }
    }

    /**
     * Try to get the user's UUID.
     */
    private static UuidNameResponse uuidFromUsername(String username) throws IOException {
        HttpResponse<String> str = Unirest.get(UUID_URL + username).asString();
        if (!str.isSuccess() || str.getStatus() != 200) {
            System.err.println("Could not get UUID for user '" + username + "'. Status: " + str.getStatus());
            throw new IOException("Cannot find username");
        }

        return new Gson().fromJson(str.getBody(), UuidNameResponse.class);
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
                ", name='" + name + '\'' +
                ", isValid=" + isValid + '\'' +
                ", accessToken='" + accessToken +
                '}';
    }

    public String getUsername() {
        return name;
    }

    private static class UuidNameResponse {
        String id, name;
    }
}



