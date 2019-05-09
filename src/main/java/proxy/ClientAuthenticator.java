package proxy;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import proxy.json.AuthDetails;
import proxy.json.LauncherProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientAuthenticator {
    static int STATUS_SUCCESS = 204;

    LauncherProfiles profiles;
    public ClientAuthenticator() {
        Path p = Paths.get(System.getenv("APPDATA"), ".minecraft", "launcher_profiles.json");
        String file = "";
        try {
             file = String.join("\n", Files.readAllLines(p));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot read launcher_profiles.json");
        }

        Gson g = new Gson();
        profiles = g.fromJson(file, LauncherProfiles.class);
    }

    public void makeRequest(String hash) throws UnirestException {
        AuthDetails details = profiles.getAuthDetails();

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", details.getAccessToken());
        body.put("selectedProfile", details.getUuid());
        body.put("serverId", hash);


        HttpResponse<String> str = Unirest.post("https://sessionserver.mojang.com/session/minecraft/join")
            .header("Content-Type", "application/json")
            .body(new Gson().toJson(body))
            .asString();

        if (str.getStatus() != STATUS_SUCCESS) {
            throw new RuntimeException("Client not authenticated! " + str.getBody());
        }
    }
}
