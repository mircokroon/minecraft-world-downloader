package proxy;

import game.NetworkMode;
import packets.DataReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import static util.ExceptionHandling.attempt;

/**
 * Proxy server class, handles receiving of data and forwarding it to the right places.
 */
public class ProxyServer extends Thread {
    private final ConnectionDetails connectionDetails;
    private final ConnectionManager connectionManager;

    private DataReader onServerBoundPacket;
    private DataReader onClientBoundPacket;

    public ProxyServer(ConnectionManager connectionManager, ConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
        this.connectionManager = connectionManager;
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
        setName("Proxy");
        
        String friendlyHost = connectionDetails.getFriendlyHost();
        System.out.println("Starting proxy for " + friendlyHost + ". Make sure to connect to localhost:" + connectionDetails.getPortLocal() + " instead of the regular server address.");

        // Create a ServerSocket to listen for connections with
        AtomicReference<ServerSocket> ss = new AtomicReference<>();
        attempt(() -> ss.set(connectionDetails.getServerSocket()), (ex) -> {
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
                connectionManager.getEncryptionManager().setStreamToClient(streamToClient);

                // If the server cannot connect, close client connection
                attempt(() -> server.set(connectionDetails.getClientSocket()), (ex) -> {
                    System.err.println("Cannot connect to " + friendlyHost + ". The server may be down or on a different address. (" + ex.getClass().getCanonicalName() + ")");
                    attempt(client.get()::close);
                });

                final InputStream streamFromServer = server.get().getInputStream();
                final OutputStream streamToServer = server.get().getOutputStream();
                connectionManager.getEncryptionManager().setStreamToServer(streamToServer);

                // start client listener thread
                Thread clientListener = new Thread(() -> {
                    connectionManager.setMode(NetworkMode.HANDSHAKE);
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
                        connectionManager.reset();
                    });
                    // the client closed the connection to us, so close our connection to the server.
                    attempt(streamToServer::close);
                }, "Proxy Client Listener");
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
                    connectionManager.reset();
                });

                // The server closed its connection to us, so we close our connection to our client.
                streamToClient.close();
            }, (ex) -> {
                if (server.get() != null) { attempt(server.get()::close); }
                if (client.get() != null) { attempt(client.get()::close); }
            });
        }
    }
}
