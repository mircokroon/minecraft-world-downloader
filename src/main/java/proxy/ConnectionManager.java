package proxy;

import config.Config;
import game.NetworkMode;
import game.data.WorldManager;
import game.protocol.HandshakeProtocol;
import game.protocol.LoginProtocol;
import game.protocol.StatusProtocol;
import packets.DataReader;
import packets.handler.*;

/**
 * Class to manage the connection status.
 */
public class ConnectionManager {
    private DataReader serverBoundDataReader;
    private DataReader clientBoundDataReader;
    private EncryptionManager encryptionManager;
    private CompressionManager compressionManager;

    private NetworkMode mode = NetworkMode.STATUS;

    public NetworkMode getMode() {
        return mode;
    }

    public void setMode(NetworkMode mode) {
        this.mode = mode;

        switch (mode) {
            case STATUS:
                PacketHandler.setProtocol(new StatusProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundStatusPacketHandler(this));
                clientBoundDataReader.setPacketHandler(new ClientBoundStatusPacketHandler(this));
                break;
            case LOGIN:
                PacketHandler.setProtocol(new LoginProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundLoginPacketHandler(this));
                clientBoundDataReader.setPacketHandler(new ClientBoundLoginPacketHandler(this));
                break;
            case GAME:
                PacketHandler.setProtocol(Config.getGameProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundGamePacketHandler(this));
                clientBoundDataReader.setPacketHandler(ClientBoundGamePacketHandler.of(this));
                break;
            case HANDSHAKE:
                PacketHandler.setProtocol(new HandshakeProtocol());
                serverBoundDataReader.setPacketHandler(new ServerBoundHandshakePacketHandler(this));
                clientBoundDataReader.setPacketHandler(new ClientBoundHandshakePacketHandler(this));
                break;
        }
    }

    /**
     * Starts the proxy.
     */
    public void startProxy() {
        compressionManager = new CompressionManager();
        encryptionManager = new EncryptionManager(compressionManager);
        serverBoundDataReader = DataReader.serverBound(encryptionManager);
        clientBoundDataReader = DataReader.clientBound(encryptionManager);

        setMode(NetworkMode.HANDSHAKE);

        ProxyServer proxy = new ProxyServer(this, Config.getConnectionDetails());
        proxy.runServer(serverBoundDataReader, clientBoundDataReader);

        Config.registerPacketInjector(this.getEncryptionManager().getPacketInjector());
    }

    /**
     * Reset the connection when its lost.
     */
    public void reset() {
        encryptionManager.reset();
        compressionManager.reset();
        serverBoundDataReader.reset();
        clientBoundDataReader.reset();
        setMode(NetworkMode.HANDSHAKE);
        WorldManager.getInstance().resetConnection();
    }

    public EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public CompressionManager getCompressionManager() {
        return compressionManager;
    }
}
