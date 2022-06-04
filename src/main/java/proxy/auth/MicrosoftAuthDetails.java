package proxy.auth;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public class MicrosoftAuthDetails {
    // this should probably be obfuscated but not worth the effort
    private final static String CLIENT_ID = "99ef6720-81b8-4bf7-a653-d6f429f1cea3";
    
    public static final String LOGIN_URL = "https://login.live.com/oauth20_authorize.srf" +
        "?client_id=" + CLIENT_ID +
        "&response_type=code" +
        "&scope=XboxLive.signin%20offline_access" +
        "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";

    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";
    public static final String REDIRECT_SUFFIX = REDIRECT_URL + "?code=";

    private transient String authCode;
    
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

    public MicrosoftAuthDetails(String authCode) {
        this.authCode = authCode;
    }

    private void refresh() {
        LocalDateTime now = LocalDateTime.now();
        if (microsoftAccessToken == null || microsoftExpiration.isAfter(now)) {
            acquireMicrosoftToken();
            System.out.println("mst " + microsoftAccessToken);
        }

        if (xboxLiveToken == null || xboxLiveExpiration.isAfter(now)) {
            acquireXboxLiveToken();
            System.out.println("xblt " + microsoftAccessToken);
        }

        if (xboxSecurityToken == null || xboxSecurityExpiration.isAfter(now)) {
            acquireXboxSecurityToken();
            System.out.println("xtsx " + microsoftAccessToken);
        }

        if (minecraftAccessToken == null || minecraftAccessExpiration.isAfter(now)) {
            acquireMinecraftAccessToken();
            System.out.println("mct " + microsoftAccessToken);
        }

        authDetails = AuthDetails.fromAccessToken(this.minecraftAccessToken);

        System.out.println(authDetails);
    }

    public static MicrosoftAuthDetails fromCode(String authCode) {
        MicrosoftAuthDetails msAuth = new MicrosoftAuthDetails(authCode);
        msAuth.refresh();
        return msAuth;
    }

    private void acquireMicrosoftToken() {
        MultipartBody body = Unirest.post("https://login.live.com/oauth20_token.srf")
            .contentType("application/x-www-form-urlencoded")
            .field("client_id", CLIENT_ID)
            .field("redirect_uri", REDIRECT_URL);
//            .field("scope", "service::user.auth.xboxlive.com::MBI_SSL");

        if (this.microsoftRefreshToken == null) {
            body = body.field("code", this.authCode)
                .field("grant_type", "authorization_code");
        } else {
            body = body.field("refresh_token", this.microsoftRefreshToken)
                .field("grant_type", "refresh_token");
        }
        System.out.println(body.getBody().get().multiParts());
        System.out.println(body.getHeaders().all().toString());

        HttpResponse<JsonNode> res = body.asJson();
        if (!res.isSuccess()) {
            // TODO: handle failure
            System.out.println(res.getStatus() + " :: " + res.getStatusText());
        }

        this.microsoftAccessToken = res.getBody().getObject().getString("access_token");
        this.microsoftRefreshToken = res.getBody().getObject().getString("refresh_token");

        int expiresIn = res.getBody().getObject().getInt("expires_in");
        this.microsoftExpiration = LocalDateTime.now().plusSeconds(expiresIn);
    }

    private void acquireXboxLiveToken() {
        HttpResponse<JsonNode> res = Unirest.post("https://user.auth.xboxlive.com/user/authenticate")
            .body(new XboxLiveBody(this.microsoftAccessToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();

        if (!res.isSuccess()) {
            // TODO: handle failure
            System.out.println(res.getStatus() + " :: " + res.getStatusText());
        }

        JSONObject jso = res.getBody().getObject();
        System.out.println(jso);
        this.xboxLiveToken = jso.getString("Token");
        this.userHash = jso.getJSONObject("DisplayClaims")
            .getJSONArray("xui")
            .getJSONObject(0)
            .getString("uhs");
        this.xboxLiveExpiration = LocalDateTime.parse(jso.getString("NotAfter").split("\\.")[0]);
    }

    private void acquireXboxSecurityToken() {
        //  TODO: handle realms token
        HttpResponse<JsonNode> res = Unirest.post("https://xsts.auth.xboxlive.com/xsts/authorize")
            .body(new XboxSecurityBody(this.xboxLiveToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();



        if (!res.isSuccess()) {
            // TODO: handle failure
            System.out.println(res.getStatus() + " :: " + res.getStatusText());
        }

        JSONObject jso = res.getBody().getObject();
        this.xboxSecurityToken = jso.getString("Token");
        this.xboxSecurityExpiration = LocalDateTime.parse(jso.getString("NotAfter").split("\\.")[0]);
    }

    private void acquireMinecraftAccessToken() {
        HttpResponse<JsonNode> res = Unirest.post("https://api.minecraftservices.com/authentication/login_with_xbox")
            .body(new MinecraftAuthBody(this.userHash, this.xboxSecurityToken).toString())
            .contentType("application/json")
            .accept("application/json")
            .asJson();

        if (!res.isSuccess()) {
            // TODO: handle failure
            System.out.println(res.getStatus() + " :: " + res.getStatusText());
        }

        JSONObject jso = res.getBody().getObject();
        this.minecraftAccessToken = jso.getString("access_token");
        this.minecraftAccessExpiration = LocalDateTime.now().plusSeconds(jso.getInt("expires_in"));
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
