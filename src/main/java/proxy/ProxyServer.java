package proxy;

import game.Game;
import game.NetworkMode;
import packets.DataReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy server class, handles receiving of data and forwarding it to the right places.
 */
public class ProxyServer extends Thread {
    private int portRemote;
    private int portLocal;
    private String host;

    private DataReader onServerBoundPacket;
    private DataReader onClientBoundPacket;

    /**
     * Initialise the proxy server class.
     * @param portRemote the port of the remote server (the 'real' server)
     * @param portLocal  the port of the local server (the proxy server)
     * @param host       the address of the remote server (the 'real' server)
     */
    public ProxyServer(int portRemote, int portLocal, String host) {
        this.portRemote = portRemote;
        this.portLocal = portLocal;
        this.host = host;
    }

    /**
     * Run the proxy server. This method does not return.
     * @param onServerBoundPacket data reader for client -> server traffic
     * @param onClientBoundPacket data reader for server -> client traffic
     */
    public void runServer(DataReader onServerBoundPacket, DataReader onClientBoundPacket) {
        this.onClientBoundPacket = onClientBoundPacket;
        this.onServerBoundPacket = onServerBoundPacket;
        this.start();
        this.setPriority(10);
    }

    @Override
    public void run() {
        System.out.println("Starting proxy for " + host + ":" + portRemote + ". Make sure to connect to localhost:" + portLocal + " instead of the regular server address.");

        // Create a ServerSocket to listen for connections with
        AtomicReference<ServerSocket> ss = new AtomicReference<>();
        attempt(() -> ss.set(new ServerSocket(portLocal)), (ex) -> {
            ex.printStackTrace();
            System.exit(1);
        });

        final byte[] request = new byte[4096];
        final byte[] reply = new byte[4096];

        while (true) {
            AtomicReference<Socket> client = new AtomicReference<>();
            AtomicReference<Socket> server = new AtomicReference<>();

            attempt(() -> {
                // Wait for a connection on the local port
                client.set(ss.get().accept());

                final InputStream streamFromClient = client.get().getInputStream();
                final OutputStream streamToClient = client.get().getOutputStream();
                Game.getEncryptionManager().setStreamToClient(streamToClient);

                // If the server cannot connect, close client connection
                attempt(() -> server.set(new Socket(host, portRemote)), (ex) -> {
                    System.out.println("Cannot connect to host: ");
                    ex.printStackTrace();

                    attempt(client.get()::close);
                });

                final InputStream streamFromServer = server.get().getInputStream();
                final OutputStream streamToServer = server.get().getOutputStream();
                Game.getEncryptionManager().setStreamToServer(streamToServer);

                // start client listener thread
                Thread clientListener = new Thread(() -> {
                    Game.setMode(NetworkMode.HANDSHAKE);
                    attempt(() -> {
                        int bytesRead;
                        while ((bytesRead = streamFromClient.read(request)) != -1) {
                            onServerBoundPacket.pushData(request, bytesRead);
                        }
                    }, (ex) -> {
                        Throwable cause = ex.getCause();
                        if (cause != null) {
                            cause.printStackTrace();
                        }
                        System.out.println("Server probably disconnected. Waiting for new connection...");
                        Game.reset();
                    });
                    // the client closed the connection to us, so close our connection to the server.
                    attempt(streamToServer::close);
                });
                clientListener.start();
                clientListener.setPriority(10);

                // listen to messages from server
                attempt(() -> {
                    int bytesRead;
                    while ((bytesRead = streamFromServer.read(reply)) != -1) {
                        onClientBoundPacket.pushData(reply, bytesRead);
                    }
                }, (ex) -> {
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        cause.printStackTrace();
                    }
                    System.out.println("Client probably disconnected. Waiting for new connection...");
                    Game.reset();
                });

                // The server closed its connection to us, so we close our connection to our client.
                streamToClient.close();
            }, (ex) -> {
                if (server.get() != null) { attempt(server.get()::close); }
                if (client.get() != null) { attempt(client.get()::close); }
            });
        }
    }

    /**
     * Simple method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r, IExceptionConsumer failure) {
        try {
            r.run();
        } catch (Exception ex) {
            failure.consume(ex);
        }
    }

    /**
     * Simple method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r) {
        attempt(r, Throwable::printStackTrace);
    }
}
