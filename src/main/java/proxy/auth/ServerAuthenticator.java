package proxy.auth;


import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import static util.PrintUtils.devPrint;

public class ServerAuthenticator {
    private static String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private final String username;


    public ServerAuthenticator(String username) {
        this.username = username;
    }

    /**
     * Verify the user that is connecting to us is actually the user who we just authenticated with Mojang, otherwise
     * we could be giving a malicious user access to our account on the server we are proxying. The user would
     * also have to supply a username matching ours so it should never be accidentally attempted.
     * @param hash hash based on the server information.
     */
    public void makeRequest(String hash) throws UnirestException {
        HttpResponse<JsonNode> str = Unirest.get(AUTH_URL)
            .queryString("username", username)
            .queryString("serverId", hash)
            .asJson();

        if (str.getStatus() != 200) {
            System.out.println("WARNING: Connection attempt by using pretending to be you! Closing connection.");
            throw new RuntimeException("Server could not authenticate client! " + str.getBody());
        } else {
            devPrint("User identity confirmed with Mojang.");
        }
    }
}
