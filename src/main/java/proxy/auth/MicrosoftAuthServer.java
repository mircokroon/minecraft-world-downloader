package proxy.auth;

import static util.ExceptionHandling.attempt;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Runs a webserver that handles the OAuth callback.
 */
public class MicrosoftAuthServer extends NanoHTTPD {
    public static final String PATH = "/world-downloader-auth-complete";
    private static final String SHORT_PATH = "/login";
    private String shortUrl = "http://localhost:%d" + SHORT_PATH;

    private BiConsumer<String, Integer> authCodeHandler;
    public MicrosoftAuthServer(Consumer<String> onStart, BiConsumer<String, Integer> authCodeHandler) throws IOException {
        super(getSocket());
        this.authCodeHandler = authCodeHandler;

        start();
        this.shortUrl = String.format(shortUrl, this.getListeningPort());
        onStart.accept(shortUrl);
    }

    /**
     * Get a free socket by opening a server socket with port 0. If this fails just use port 8080
     * as a default,
     */
    private static int getSocket() {
        int socket = 8080;
        try (ServerSocket ss = new ServerSocket(0)) {
            socket = ss.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }


    /**
     * Handle requests to server.
     */
    @Override
    public Response serve(IHTTPSession session) {
        if (session.getUri().equals(SHORT_PATH)) {
            Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
            r.addHeader("Location", MicrosoftAuthHandler.getLoginUrl(this.getListeningPort()));
            return r;
        }
        if (!session.getUri().equals(PATH)) {
            return null;
        }
        if (authCodeHandler != null) {
            String code = session.getQueryParameterString().replaceFirst("code=", "");
            int port = this.getListeningPort();

            authCodeHandler.accept(code, port);
            authCodeHandler = null;
        }

        this.stopDelayed();
        String html = """
            <!DOCTYPE html>
            <html>
              <body>
                  <div>Login complete. You can close this page.</div>
                  <style>
                      * { background-color: #333333; }
                      div {
                          text-align: center;
                          line-height: 90vh;
                          font-family: sans-serif;
                          font-size: 1.4em;
                          color: white;
                      }
                  </style>
              </body>
            </html>
            """;
        return newFixedLengthResponse(html);
    }

    /**
     * Stop server after a few seconds. Can't do it immediately or the user will get an ugly error.
     */
    private void stopDelayed() {
        Thread stopThread = new Thread(() -> {
            attempt(() -> Thread.sleep(5000));
            this.stop();
        });
        stopThread.start();
    }

    public String getShortUrl() {
        return shortUrl;
    }
}
