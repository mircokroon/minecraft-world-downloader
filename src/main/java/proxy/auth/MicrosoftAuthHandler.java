package proxy.auth;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Handles logging in using Microsoft authentication.
 */
public class MicrosoftAuthHandler {
    // this should probably be obfuscated but not worth the effort
    private final static String CLIENT_ID = "99ef6720-81b8-4bf7-a653-d6f429f1cea3";

    private static final String REDIRECT_URL = "http://localhost:%d/world-downloader-auth-complete";
    public static final String LOGIN_URL = "https://login.live.com/oauth20_authorize.srf" +
        "?client_id=" + CLIENT_ID +
        "&response_type=code" +
        "&scope=XboxLive.signin%%20offline_access" +
        "&redirect_uri=" + REDIRECT_URL +
        "&prompt=select_account";

    public static final String REDIRECT_SUFFIX = REDIRECT_URL + "?code=";

    private transient String authCode;

    private final int usedPort;
    private String userHash;
    private String microsoftAccessToken;
    private String microsoftRefreshToken;
    private LocalDateTime microsoftExpiration = LocalDateTime.MIN;

    private String xboxLiveToken;
    private LocalDateTime xboxLiveExpiration = LocalDateTime.MIN;

    private String xboxSecurityToken;
    private LocalDateTime xboxSecurityExpiration = LocalDateTime.MIN;

    private String minecraftAccessToken;
    private LocalDateTime minecraftAccessExpiration = LocalDateTime.MIN;

    private transient AuthDetails authDetails;

    public MicrosoftAuthHandler(String authCode, int usedPort) {
        this.authCode = authCode;
        this.usedPort = usedPort;
    }

    public static String getLoginUrl(int port) {
        return String.format(LOGIN_URL, port);
    }

    private String getRedirectUrl() {
        return String.format(REDIRECT_URL, usedPort);
    }

    private void refresh() {
        LocalDateTime now = LocalDateTime.now();
        if (microsoftAccessToken == null || microsoftExpiration.isBefore(now)) {
            acquireMicrosoftToken();
        }

        if (xboxLiveToken == null || xboxLiveExpiration.isBefore(now)) {
            acquireXboxLiveToken();
        }

        if (xboxSecurityToken == null || xboxSecurityExpiration.isBefore(now)) {
            acquireXboxSecurityToken();
        }

        if (minecraftAccessToken == null || minecraftAccessExpiration.isBefore(now)) {
            acquireMinecraftAccessToken();
        }
    }



    public static MicrosoftAuthHandler fromCode(String authCode, int usedPort) {
        MicrosoftAuthHandler msAuth = new MicrosoftAuthHandler(authCode, usedPort);
        msAuth.refresh();
        return msAuth;
    }

    private void acquireMicrosoftToken() {
        MultipartBody body = Unirest.post("https://login.live.com/oauth20_token.srf")
            .contentType("application/x-www-form-urlencoded")
            .field("client_id", CLIENT_ID)
            .field("redirect_uri", getRedirectUrl());

        if (this.microsoftRefreshToken == null) {
            body = body.field("code", this.authCode)
                .field("grant_type", "authorization_code");
        } else {
            body = body.field("refresh_token", this.microsoftRefreshToken)
                .field("grant_type", "refresh_token");
        }

        HttpResponse<JsonNode> res = body.asJson();
        if (!res.isSuccess()) {
            System.out.println(res.getBody().toString());
            throw new MinecraftAuthenticationException("Cannot get Microsoft token. Status: " + res.getStatus());
        }

        this.microsoftAccessToken = res.getBody().getObject().getString("access_token");
        this.microsoftRefreshToken = res.getBody().getObject().getString("refresh_token");

        int expiresIn = res.getBody().getObject().getInt("expires_in");
        this.microsoftExpiration = LocalDateTime.now().plusSeconds(expiresIn);

        // invalidate others
        xboxLiveExpiration = LocalDateTime.MIN;
        xboxSecurityExpiration = LocalDateTime.MIN;
        minecraftAccessExpiration = LocalDateTime.MIN;
    }

    private void acquireXboxLiveToken() {
        HttpResponse<JsonNode> res = Unirest.post("https://user.auth.xboxlive.com/user/authenticate")
            .body(new XboxLiveBody(this.microsoftAccessToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();

        if (!res.isSuccess()) {
            throw new MinecraftAuthenticationException("Cannot get Xbox Live token. Status: " + res.getStatus());
        }

        JSONObject jso = res.getBody().getObject();

        this.xboxLiveToken = jso.getString("Token");
        this.userHash = jso.getJSONObject("DisplayClaims")
            .getJSONArray("xui")
            .getJSONObject(0)
            .getString("uhs");
        this.xboxLiveExpiration = LocalDateTime.parse(jso.getString("NotAfter").split("\\.")[0]);

        // invalidate others
        xboxSecurityExpiration = LocalDateTime.MIN;
        minecraftAccessExpiration = LocalDateTime.MIN;
    }

    private void acquireXboxSecurityToken() {
        //  TODO: handle realms token
        HttpResponse<JsonNode> res = Unirest.post("https://xsts.auth.xboxlive.com/xsts/authorize")
            .body(new XboxSecurityBody(this.xboxLiveToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();

        if (!res.isSuccess()) {
            throw new MinecraftAuthenticationException("Cannot get XS token. Status: " + res.getStatus());
        }

        JSONObject jso = res.getBody().getObject();
        this.xboxSecurityToken = jso.getString("Token");
        this.xboxSecurityExpiration = LocalDateTime.parse(jso.getString("NotAfter").split("\\.")[0]);

        // invalidate others
        minecraftAccessExpiration = LocalDateTime.MIN;
    }

    private void acquireMinecraftAccessToken() {
        HttpResponse<JsonNode> res = Unirest.post("https://api.minecraftservices.com/authentication/login_with_xbox")
            .body(new MinecraftAuthBody(this.userHash, this.xboxSecurityToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();

        if (!res.isSuccess()) {
            throw new MinecraftAuthenticationException("Cannot get Minecraft token. Status: " + res.getStatus());
        }

        JSONObject jso = res.getBody().getObject();
        this.minecraftAccessToken = jso.getString("access_token");
        this.minecraftAccessExpiration = LocalDateTime.now().plusSeconds(jso.getInt("expires_in"));
    }

    public AuthDetails getAuthDetails() {
        if (authDetails == null) {
            this.refresh();
            authDetails = AuthDetails.fromAccessToken(this.minecraftAccessToken);
        }
        return authDetails;
    }

    public boolean hasLoggedIn() {
        return (this.authCode != null && !this.authCode.isEmpty())
            || (this.microsoftRefreshToken != null && !this.microsoftRefreshToken.isEmpty());
    }


    private static class XboxLiveBody {
        String RelyingParty = "http://auth.xboxlive.com";
        String TokenType = "JWT";
        XboxLiveProperties Properties;

        public XboxLiveBody(String accessToken) {
            Properties = new XboxLiveProperties(accessToken);
        }

        private static class XboxLiveProperties {
            String AuthMethod = "RPS";
            String SiteName = "user.auth.xboxlive.com";
            String RpsTicket;

            public XboxLiveProperties(String accessToken) {
                RpsTicket = "d=" + accessToken;
            }
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    private static class XboxSecurityBody {
        String RelyingParty = "rp://api.minecraftservices.com/";
        String TokenType = "JWT";
        XboxSecurityProperties Properties;

        public XboxSecurityBody(String xboxLiveToken) {
            Properties = new XboxSecurityProperties(xboxLiveToken);
        }

        private static class XboxSecurityProperties {
            String SandboxId = "RETAIL";
            String[] UserTokens;
            public XboxSecurityProperties(String xboxLiveToken) {
                this.UserTokens = new String[] { xboxLiveToken };
            }
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    private static class MinecraftAuthBody {
        String identityToken;

        public MinecraftAuthBody(String userHash, String xboxSecurityToken) {
            this.identityToken = "XBL3.0 x=" + userHash + ";" + xboxSecurityToken;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
