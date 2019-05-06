import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class ProxyServer {
    int portRemote;
    int portLocal;
    String host;

    public ProxyServer(int portRemote, int portLocal, String host) {
        this.portRemote = portRemote;
        this.portLocal = portLocal;
        this.host = host;
    }

    /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    void runServer(IPacketHandler onServerPacket, IPacketHandler onClientPacket) {
        System.out.println("Starting proxy for " + host + ":" + portRemote + " on port " + portLocal);

        // Create a ServerSocket to listen for connections with
        AtomicReference<ServerSocket> ss = new AtomicReference<>();
        attempt(() -> ss.set(new ServerSocket(portLocal)), (ex) -> {
            ex.printStackTrace();
            System.exit(1);
        });

        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];

        while (true) {
            AtomicReference<Socket> client = new AtomicReference<>();
            AtomicReference<Socket> server = new AtomicReference<>();

            attempt(() -> {
                // Wait for a connection on the local port
                client.set(ss.get().accept());

                final ByteArrayOutputStream clientHandlerOutput = new ByteArrayOutputStream();
                final InputStream streamFromClient = client.get().getInputStream();
                final OutputStream streamToClient = client.get().getOutputStream();

                // If the server cannot connect, close client connection
                attempt(() -> server.set(new Socket(host, portRemote)), (ex) -> {
                    System.out.println("Cannot connect to host: ");
                    ex.printStackTrace();

                    attempt(client.get()::close);
                });

                final ByteArrayOutputStream serverHandlerOutput = new ByteArrayOutputStream();
                final InputStream streamFromServer = server.get().getInputStream();
                final OutputStream streamToServer = server.get().getOutputStream();

                new Thread(() -> {
                    attempt(() -> {
                        int bytesRead;
                        while ((bytesRead = streamFromClient.read(request)) != -1) {
                            onClientPacket.handle(request);
                            streamToServer.write(request, 0, bytesRead);
                            streamToServer.flush();
                        }

                    });

                    // the client closed the connection to us, so close our connection to the server.
                    attempt(streamToServer::close);
                }).start();

                attempt(() -> {
                    int bytesRead;
                    while ((bytesRead = streamFromServer.read(reply)) != -1) {
                        onServerPacket.handle(request);
                        streamToClient.write(reply, 0, bytesRead);
                        streamToClient.flush();
                    }
                }, (ex) -> System.out.println("Client probably disconnected. Waiting for new connection..."));

                // The server closed its connection to us, so we close our connection to our client.
                streamToClient.close();
            }, (ex) -> {
                if (server.get() != null) attempt(server.get()::close);
                if (client.get() != null) attempt(client.get()::close);
            });
        }
    }

    private static void attempt(IExceptionHandler r) {
        attempt(r, Throwable::printStackTrace);
    }

    private static void attempt(IExceptionHandler r, IExceptionConsumer failure) {
        try {
            r.run();
        } catch (Exception ex) {
            failure.consume(ex);
        }
    }
}
