package gui;

import config.Config;
import java.util.function.Consumer;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import proxy.auth.MicrosoftAuthHandler;

public class MsAuthController {
    public WebView webView;
    private Consumer<Exception> onError;
    private Runnable onComplete;

    public void setHandlers(Runnable onComplete, Consumer<Exception> onError) {
        this.onComplete = onComplete;
        this.onError = onError;
    }

    @FXML
    public void initialize() {
        webView.getEngine().load(MicrosoftAuthHandler.LOGIN_URL);
        webView.getEngine().setJavaScriptEnabled(true);

        // based on https://github.com/MiniDigger/MiniLauncher/
        webView.getEngine().getHistory().getEntries().addListener((ListChangeListener<WebHistory.Entry>) c -> {
            if (c.next() && c.wasAdded()) {
                for (WebHistory.Entry entry : c.getAddedSubList()) {
                    if (entry.getUrl().startsWith(MicrosoftAuthHandler.REDIRECT_SUFFIX)) {
                        String authCode = entry.getUrl().substring(entry.getUrl().indexOf("=") + 1, entry.getUrl().indexOf("&"));
                        GuiManager.closeMicrosoftLogin();

                        try {
                            MicrosoftAuthHandler msAuth = MicrosoftAuthHandler.fromCode(authCode);
                            Config.setMicrosoftAuth(msAuth);
                        } catch (Exception ex) {
                            if (onError != null) {
                                onError.accept(ex);
                            }
                        }
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }
            }
        });
    }
}
