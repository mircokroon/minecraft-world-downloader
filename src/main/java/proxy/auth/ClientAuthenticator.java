package proxy.auth;

import com.google.gson.Gson;
import gui.GuiManager;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import kong.unirest.json.JSONObject;
import proxy.EncryptionManager;

import static util.PrintUtils.devPrint;

/**
 * Handle authentication the client when joining servers.
 */
public class ClientAuthenticator extends AuthDetailsManager {
    private static final int STATUS_SUCCESS = 204;
    private static final String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/join";
    private static final String KEY_URL = "https://api.minecraftservices.com/player/certificates";

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

        GuiManager.setStatusMessage("Authenticating with Mojang servers...");

        HttpResponse<String> str = Unirest.post(AUTH_URL)
                .header("Content-Type", "application/json")
                .body(new Gson().toJson(body))
                .asString();

        GuiManager.setStatusMessage("");

        if (!str.isSuccess()) {
            throw new RuntimeException("Client not authenticated! " + str.getStatus() + ": " + str.getStatusText());
        } else {
            devPrint("Successfully authenticated user with Mojang session server.");
        }
    }

    public void getClientProfileKeyPair(EncryptionManager em) throws AuthenticationException {
        AuthDetails details;
        try {
            details = getAuthDetails();
        } catch (IOException e) {
            printAuthErrorMessage();
            throw new AuthenticationException("Cannot get valid authentication details.", e);
        }

        GuiManager.setStatusMessage("Requesting encryption keys...");

        HttpResponse<JsonNode> res = Unirest.post(KEY_URL)
            .header("Authorization", "Bearer " + details.getAccessToken())
            .asJson();

        if (!res.isSuccess()) {
            GuiManager.setStatusMessage("");
            throw new RuntimeException("Cannot get client public key: " + res.getStatus() + ": " + res.getStatusText());
        }

        JsonNode json = res.getBody();

        JSONObject keyPair = json.getObject().getJSONObject("keyPair");
        String privateKey = keyPair.getString("privateKey");
        String publicKey = keyPair.getString("publicKey");

        // for some reason the key format doesn't appear to actually follow the specification? it
        // seems to follow the PKCS8 format but with PKCS1 begin/end, if we remove the "RSA" part
        // we can actually parse them normally
        privateKey = privateKey.replace("RSA ", "");
        publicKey = publicKey.replace("RSA ", "");

        em.setClientProfileKeyPair(privateKey, publicKey);

        GuiManager.setStatusMessage("");
    }
}
