package proxy.auth;

import static util.ExceptionHandling.attempt;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Runs a webserver that handles the OAuth callback.
 */
public class MicrosoftAuthServer extends NanoHTTPD {
    public static final String PATH = "/world-downloader-auth-complete";

    private BiConsumer<String, Integer> authCodeHandler;
    public MicrosoftAuthServer(IntConsumer onStart, BiConsumer<String, Integer> authCodeHandler) throws IOException {
        super(getSocket());
        this.authCodeHandler = authCodeHandler;

        start();
        onStart.accept(this.getListeningPort());
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
                      div {
                          text-align: center;
                          line-height: 90vh;
                          font-family: sans-serif;
                          font-size: 1.4em;
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
}
