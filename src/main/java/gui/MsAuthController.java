package gui;

import config.Config;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import proxy.auth.MicrosoftAuthDetails;

public class MsAuthController {


    public WebView webView;

    @FXML
    public void initialize() {
        webView.getEngine().load(MicrosoftAuthDetails.LOGIN_URL);
        System.out.println(MicrosoftAuthDetails.LOGIN_URL);
        webView.getEngine().setJavaScriptEnabled(true);

        // based on https://github.com/MiniDigger/MiniLauncher/
        webView.getEngine().getHistory().getEntries().addListener((ListChangeListener<WebHistory.Entry>) c -> {
            if (c.next() && c.wasAdded()) {
                for (WebHistory.Entry entry : c.getAddedSubList()) {
                    if (entry.getUrl().startsWith(MicrosoftAuthDetails.REDIRECT_SUFFIX)) {
                        String authCode = entry.getUrl().substring(entry.getUrl().indexOf("=") + 1, entry.getUrl().indexOf("&"));
                        // once we got the auth code, we can turn it into a oauth token
                        System.out.println("authCode: " + authCode);
                        MicrosoftAuthDetails msAuth = MicrosoftAuthDetails.fromCode(authCode);

                        GuiManager.closeWebView();
                        Config.getInstance().setMicrosoftAuth(msAuth);
                    }
                }
            }
        });
    }
}
