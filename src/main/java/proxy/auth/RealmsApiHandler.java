package proxy.auth;

import javax.security.sasl.AuthenticationException;
import kong.unirest.Unirest;

import java.io.IOException;
import java.util.function.Consumer;

import static util.ExceptionHandling.attempt;

/**
 * Handle requests to the realms API. 
 */
public class RealmsApiHandler extends ClientAuthenticator {
    private static final String REALMS_URL = "https://pc.realms.minecraft.net/";

    /**
     * Initialise the authenticator class by reading from the JSON file.
     */
    public RealmsApiHandler(String username) {
        super(username);
    }

    /**
     * Set up the basic request with the required headers.
     * @param url the relative URL.
     * @param reportErrors if true, reports errors in the console.
     * @param callback callback function so that the request can be done without blocking the UI thread.
     */
    private void realmRequest(String url, boolean reportErrors, Consumer<String> callback) {
        AuthDetails details;
        try {
            details = getAuthDetails();
        } catch (IOException e) {
            printAuthErrorMessage();
            callback.accept(null);
            return;
        }

        Unirest.get(REALMS_URL + url)
                .cookie("sid", "token:" + details.getAccessToken() + ":" + details.getUuid())
                .cookie("user", details.getUsername())
                .cookie("version", "1.12.2")  // version doesn't seem to actually matter
                .asStringAsync(res -> {
                    if (!res.isSuccess()) {
                        if (reportErrors || (res.getStatus() >= 400 && res.getStatus() < 500)) {
                            new RealmsRequestException(res.getStatus() + " - " + res.getStatusText() + " (body: " + res.getBody() + ")").printStackTrace();
                        }
                        attempt(() -> callback.accept(""));
                    } else {
                        attempt(() -> callback.accept(res.getBody()));
                    }
                });
    }

    /**
     * Request for the IP of a specific server, done by calling the join endpoint.
     */
    public void requestRealmIp(int id, Consumer<String> callback) {
        realmRequest("worlds/v1/" + id + "/join/pc", false ,callback);
    }

    /**
     * Request the full list of realms accessible to a user.
     */
    public void requestRealms(Consumer<String> callback) {
        realmRequest("worlds", true, callback);
    }

    private static class RealmsRequestException extends RuntimeException {
        public RealmsRequestException(String message) {
            super("Could not request realms. Reason: " +message);
        }
    }
}

