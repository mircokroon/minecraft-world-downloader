package proxy.auth;

import com.google.gson.Gson;
import java.io.IOException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

/**
 * Deserialization class for the launcher config file.
 */
public class AuthDetails {
    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String ACCESSTOKEN_URL = "https://api.minecraftservices.com/minecraft/profile";
    public final static AuthDetails INVALID = new AuthDetails();

    private String accessToken;
    private String uuid;
    private String name;
    private transient boolean isOutdated;

    public AuthDetails(String name, String uuid, String accessToken) {
        this.name = name;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.isOutdated = false;
    }

    private AuthDetails() {
        name = "";
        uuid = "";
        accessToken = "";
        isOutdated = false;
    }

    public AuthDetails(String accessToken) {
        this.accessToken = accessToken;
        this.isOutdated = true;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
        this.isOutdated = true;
    }

    public void update() {
        HttpResponse<String> str = Unirest.get(ACCESSTOKEN_URL)
            .header("Authorization", "Bearer " + accessToken).asString();

        if (!str.isSuccess() || str.getStatus() != 200) {
            System.err.println("Could not get details from access token'. Status: " + str.getStatus());
            throw new MinecraftAuthenticationException("Cannot get profile.");
        }
        UuidNameResponse res = new Gson().fromJson(str.getBody(), UuidNameResponse.class);
        this.uuid = res.id;
        this.name = res.name;
        this.isOutdated = false;
    }

    public boolean isValid() {
        if (this.accessToken == null || this.accessToken.equals("")) {
            return false;
        }


        try {
            this.update();
        } catch (MinecraftAuthenticationException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    public static AuthDetails fromAccessToken(String accessToken) {
        if (accessToken == null || accessToken.length() == 0) {
            return INVALID;
        }

        AuthDetails details = new AuthDetails(accessToken);
        details.update();
        return details;
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
        if (isOutdated) {
            this.update();
        }

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
                ", accessToken='" + accessToken +
                '}';
    }

    public String getUsername() {
        if (isOutdated) {
            this.update();
        }

        return name;
    }

    private static class UuidNameResponse {
        String id, name;
    }
}



