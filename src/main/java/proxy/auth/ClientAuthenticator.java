package proxy.auth;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static util.PrintUtils.devPrint;

/**
 * Handle authentication the client when joining servers.
 */
public class ClientAuthenticator extends AuthDetailsManager {
    private static final int STATUS_SUCCESS = 204;
    private static final String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/join";

    public ClientAuthenticator() { }

    public ClientAuthenticator(String username) {
        super(username);
    }

    /**
     * Make the authentication request to the Mojang session server. We need to do this as the one sent by the
     * real client will have had our 'fake' public key instead of the server's real one, and as such the server will
     * not accept the connection.
     * @param hash hash based on the server information.
     */
    public void makeRequest(String hash) throws UnirestException, AuthenticationException {
        AuthDetails details;
        try {
            details = getAuthDetails();
        } catch (IOException e) {
            printAuthErrorMessage();
            throw new AuthenticationException("Cannot get valid authentication details.", e);
        }

        Map<String, String> body = new HashMap<>();

        body.put("accessToken", details.getAccessToken());
        body.put("selectedProfile", details.getUuid());
        body.put("serverId", hash);


        HttpResponse<String> str = Unirest.post(AUTH_URL)
                .header("Content-Type", "application/json")
                .body(new Gson().toJson(body))
                .asString();

        if (str.getStatus() != STATUS_SUCCESS) {
            throw new RuntimeException("Client not authenticated! " + str.getBody());
        } else {
            devPrint("Successfully authenticated user with Mojang session server.");
        }
    }
}
