package proxy;

import game.Game;
import packets.lib.ByteQueue;
import proxy.auth.ClientAuthenticator;
import proxy.auth.ServerAuthenticator;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class to handle encryption, decryption and related masking of the proxy server.
 */
public class EncryptionManager {
    private static final String ENCRYPTION_TYPE = "AES/CFB8/NoPadding";
    public final int blockSize = 16;
    private boolean encryptionEnabled = false;
    private String serverId;
    private RSAPublicKey serverRealPublicKey;
    private byte[] serverVerifyToken;
    private byte[] clientSharedSecret;
    private Cipher clientBoundDecryptor, clientBoundEncryptor, serverBoundEncryptor, serverBoundDecryptor;
    private OutputStream streamToClient;
    private OutputStream streamToServer;
    private KeyPair serverKeyPair;
    private String username;

    {
        // generate the keypair for the fake server
        attempt(() -> {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            serverKeyPair = keyGen.generateKeyPair();
        });
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * When the server sends the client an encryption request, this method will be called to get the server's given
     * public key and call the replacement request sender.
     * @param encoded  the encoded public key in X509
     * @param token    the server's verification token
     * @param serverId the server's id (not actually used)
     */
    public void setServerEncryptionRequest(byte[] encoded, byte[] token, String serverId) {
        attempt(() -> {
            serverVerifyToken = token;
            this.serverId = serverId;

            KeyFactory kf = KeyFactory.getInstance("RSA");
            serverRealPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));

            sendReplacementEncryptionRequest();
        });
    }

    /**
     * Simple method to make exception handling cleaner.
     */
    private static void attempt(IExceptionHandler r) {
        try {
            r.run();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Encryption failure! Terminating.");
            System.exit(1);
        }
    }

    /**
     * Simple method to make exception handling cleaner.
     */
    private boolean disconnectOnError(IExceptionHandler r) {
        try {
            r.run();
        } catch (Exception ex) {
            attempt(() -> streamToServer.close());
            attempt(() -> streamToClient.close());
            return false;
        }
        return true;
    }

    /**
     * This method will send the client a self-generated public key in an encryption request. This way we will be able
     * to decrypt the client's shared secret key later on and use this to decrypt all the traffic.
     */
    private void sendReplacementEncryptionRequest() {
        List<Byte> bytes = new ArrayList<>();
        byte[] encoded = serverKeyPair.getPublic().getEncoded();
        writeVarInt(bytes, 0x01);   // packet ID
        writeString(bytes, serverId);    // server ID
        writeVarInt(bytes, encoded.length); // pub key len
        writeByteArray(bytes, encoded); // pub key
        writeVarInt(bytes, serverVerifyToken.length); // verify token len
        writeByteArray(bytes, serverVerifyToken);  // verify token
        prependPacketLength(bytes);

        attempt(() -> streamToClient(new ByteQueue(bytes)));
    }

    /**
     * Method to write a varInt to the given list. Based on: https://wiki.vg/Protocol
     * @param bytes the current list of bytes
     * @param value the value to write
     */
    private static void writeVarInt(List<Byte> bytes, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            bytes.add(temp);
        } while (value != 0);
    }

    /**
     * Method to write a string to the given list.
     * @param bytes the current list of bytes
     * @param str   the string to write
     */
    private static void writeString(List<Byte> bytes, String str) {
        final byte[][] stringBytes = {null};
        attempt(() -> stringBytes[0] = str.getBytes(StandardCharsets.UTF_8));
        writeVarInt(bytes, stringBytes[0].length);
        writeByteArray(bytes, stringBytes[0]);
    }

    /**
     * Method to write a byte array to the given list.
     * @param list  the current list of bytes
     * @param bytes the bytes to write
     */
    private static void writeByteArray(List<Byte> list, byte[] bytes) {
        for (byte b : bytes) {
            list.add(b);
        }
    }

    /**
     * Method to write a the packet length to the start of the given list.
     * @param bytes the current list of bytes
     */
    private static void prependPacketLength(List<Byte> bytes) {
        int len = bytes.size();

        List<Byte> varIntLen = new ArrayList<>(5);
        writeVarInt(varIntLen, len);
        bytes.addAll(0, varIntLen);
    }

    /**
     * Method to stream a given queue of bytes to the client.
     * @param bytes the bytes to stream
     */
    public void streamToClient(ByteQueue bytes) throws IOException {
        streamTo(streamToClient, bytes, this::clientBoundEncrypt);
    }

    /**
     * Method to stream a queue of bytes to a given output stream. The stream will be encrypted if encryption has been
     * enabled.
     * @param stream  the stream to write to
     * @param bytes   the bytes to write
     * @param encrypt the encryption operator
     */
    private void streamTo(OutputStream stream, ByteQueue bytes, UnaryOperator<byte[]> encrypt) throws IOException {
        byte[] b = new byte[bytes.size()];
        bytes.copyTo(b);

        byte[] encrypted = encrypt.apply(b);

        stream.write(encrypted, 0, encrypted.length);
        stream.flush();
    }

    /**
     * Encrypts a given byte array using the encryption stream for the client-side.
     */
    private byte[] clientBoundEncrypt(byte[] bytes) {
        return encrypt(bytes, clientBoundEncryptor);
    }

    /**
     * Encrypts a given byte array using the the given encryptor, if encryption has been enabled.
     */
    private byte[] encrypt(byte[] bytes, Cipher encryptor) {
        if (!encryptionEnabled) { return bytes; }

        try {
            return encryptor.update(bytes);
        } catch (Exception ex) {
            throw new RuntimeException("Could not encrypt stream!", ex);
        }
    }

    /**
     * Called to intercept the client's encryption confirmation. Because we intercepted the server's real public key,
     * we need to now decrypt the given shared secret key (and token) and re-encrypt it using the real public key.
     * Through this method we can learn the shared secret key.
     * @param encryptedSharedSecret the encrypted shared secret key
     * @param token                 the verification token
     */
    public void setClientEncryptionConfirmation(byte[] encryptedSharedSecret, byte[] token) {
        attempt(() -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
            byte[] decryptedToken = cipher.doFinal(token);

            if (!Arrays.equals(decryptedToken, serverVerifyToken)) {
                throw new RuntimeException("Token could not be verified!");
            }

            clientSharedSecret = cipher.doFinal(encryptedSharedSecret);

            sendReplacementEncryptionConfirmation();
        });
    }

    /**
     * This method first authenticates with the Mojang servers, then sends the server a replacement encryption
     * confirmation message. This confirmation will include the client's real shared secret but it will be encrypted
     * using the server's real public key instead of our own. After this, encryption will be enabled for all
     * future traffic.
     */
    private void sendReplacementEncryptionConfirmation() {
        // authenticate the client so that the remote server will accept us
        boolean client = disconnectOnError(() -> new ClientAuthenticator().makeRequest(generateServerHash()));
        if (!client) { return; }

        // verify the connecting client connection is who he claims to be
        boolean server = disconnectOnError(() -> new ServerAuthenticator(username).makeRequest(generateServerHash()));
        if (!server) { return; }

        // encryption confirmation
        attempt(() -> {
            List<Byte> bytes = new ArrayList<>();

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverRealPublicKey);
            byte[] sharedSecret = cipher.doFinal(clientSharedSecret);
            byte[] verifyToken = cipher.doFinal(serverVerifyToken);

            writeVarInt(bytes, 0x01);
            writeVarInt(bytes, sharedSecret.length);
            writeByteArray(bytes, sharedSecret);
            writeVarInt(bytes, verifyToken.length);
            writeByteArray(bytes, verifyToken);
            prependPacketLength(bytes);

            streamToServer(new ByteQueue(bytes));

            enableEncryption();
        });
    }

    /**
     * Generate the server hash to be used for the Mojang session server.
     * @return the generated hash. See https://wiki.vg/Protocol
     */
    private String generateServerHash() {
        AtomicReference<MessageDigest> sha1 = new AtomicReference<>();
        attempt(() -> sha1.set(MessageDigest.getInstance("SHA1")));

        sha1.get().update(serverId.getBytes(StandardCharsets.US_ASCII));
        sha1.get().update(clientSharedSecret);
        sha1.get().update(serverRealPublicKey.getEncoded());

        return new BigInteger(sha1.get().digest()).toString(16);
    }

    public void streamToServer(ByteQueue bytes) throws IOException {
        // System.out.println("Writing bytes to server: " + bytes.size() + " :: " + bytes);
        streamTo(streamToServer, bytes, this::serverBoundEncrypt);
    }

    /**
     * Enable encryption for all future packets. We need to create four cyphers: a decryptor and encryptor for the
     * client-bound packets, and a decryptor and encryptor for the server-bound packets. As the cypher is continuous
     * and not per-packet we cannot re-use the streams between the client and server despite them having the same key.
     */
    private void enableEncryption() {
        attempt(() -> {
            IvParameterSpec ivspec = new IvParameterSpec(clientSharedSecret);
            SecretKeySpec k = new SecretKeySpec(clientSharedSecret, "AES");
            clientBoundEncryptor = Cipher.getInstance(ENCRYPTION_TYPE);
            clientBoundEncryptor.init(Cipher.ENCRYPT_MODE, k, ivspec);

            clientBoundDecryptor = Cipher.getInstance(ENCRYPTION_TYPE);
            clientBoundDecryptor.init(Cipher.DECRYPT_MODE, k, ivspec);

            serverBoundEncryptor = Cipher.getInstance(ENCRYPTION_TYPE);
            serverBoundEncryptor.init(Cipher.ENCRYPT_MODE, k, ivspec);

            serverBoundDecryptor = Cipher.getInstance(ENCRYPTION_TYPE);
            serverBoundDecryptor.init(Cipher.DECRYPT_MODE, k, ivspec);

            encryptionEnabled = true;
            System.out.println("Enabled encryption");
        });
    }

    private byte[] serverBoundEncrypt(byte[] bytes) {
        return encrypt(bytes, serverBoundEncryptor);
    }

    public void setStreamToClient(OutputStream streamToClient) {
        this.streamToClient = streamToClient;
    }

    public void setStreamToServer(OutputStream streamToServer) {
        this.streamToServer = streamToServer;
    }

    public byte[] serverBoundDecrypt(byte[] bytes) {
        return decrypt(bytes, serverBoundDecryptor);
    }

    private byte[] decrypt(byte[] bytes, Cipher decryptor) {
        if (!encryptionEnabled) { return bytes; }

        try {
            return decryptor.update(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public byte[] clientBoundDecrypt(byte[] bytes) {
        return decrypt(bytes, clientBoundDecryptor);
    }

    public void reset() {
        encryptionEnabled = false;
    }

    /**
     * Send a masked handshake packet to the client. We will intercept the real one as it has the hostname and port of
     * our proxy instead of the ones from the real server. Vanilla clients will accept this but some servers verify
     * the hostname before allowing a connection.
     * @param protocolVersion the version of the connection protocol
     * @param nextMode        the next connection mode (login or status)
     */
    public void sendMaskedHandshake(int protocolVersion, int nextMode) {
        attempt(() -> {
            List<Byte> bytes = new ArrayList<>();

            writeVarInt(bytes, 0);
            writeVarInt(bytes, protocolVersion);
            writeString(bytes, Game.getHost());
            writeShort(bytes, Game.getPortRemote());
            writeVarInt(bytes, nextMode);
            prependPacketLength(bytes);

            System.out.format(
                "Performed handshake with %s:%d, protocol version %d :: next state: %s\n",
                Game.getHost(),
                Game.getPortRemote(),
                protocolVersion,
                nextMode == 1 ? "status" : "login"
            );

            streamToServer(new ByteQueue(bytes));
        });
    }

    /**
     * Write a short to the given byte list.
     * @param bytes    the list to write to
     * @param shortVal the value of the short
     */
    private static void writeShort(List<Byte> bytes, int shortVal) {
        bytes.add((byte) ((shortVal >>> 8) & 0xFF));
        bytes.add((byte) ((shortVal) & 0xFF));
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
