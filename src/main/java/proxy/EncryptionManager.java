package proxy;

import config.Config;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import packets.builder.PacketBuilder;
import packets.lib.ByteQueue;
import proxy.auth.ClientAuthenticator;
import proxy.auth.ServerAuthenticator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static util.PrintUtils.devPrintFormat;

/**
 * Class to handle encryption, decryption and related masking of the proxy server.
 */
public class EncryptionManager {
    private static final String ENCRYPTION_TYPE = "AES/CFB8/NoPadding";
    private boolean encryptionEnabled = false;
    private String serverId;
    private RSAPublicKey serverRealPublicKey;
    private byte[] nonce;
    private byte[] clientSharedSecret;
    private Cipher clientBoundDecryptor, clientBoundEncryptor, serverBoundEncryptor, serverBoundDecryptor;
    private OutputStream streamToClient;
    private OutputStream streamToServer;
    private KeyPair serverKeyPair;
    private KeyPair clientProfileKeyPair;
    private String username;
    private final ConcurrentLinkedQueue<ByteQueue> insertedPackets;
    private final CompressionManager compressionManager;
    private final ClientAuthenticator clientAuthenticator;
    private final JcaPEMKeyConverter keyConverter;

    private RSAPublicKey clientProfilePublicKey;

    {
        Security.addProvider(new BouncyCastleProvider());
        keyConverter = new JcaPEMKeyConverter().setProvider(new BouncyCastleProvider());

        // generate the keypair for the local server
        attempt(() -> {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            serverKeyPair = keyGen.generateKeyPair();
        });
    }

    public EncryptionManager(CompressionManager compressionManager) {
        this.compressionManager = compressionManager;
        this.insertedPackets = new ConcurrentLinkedQueue<>();
        this.clientAuthenticator = new ClientAuthenticator();
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Adds a packet to the queue. This queue is checked whenever a packet is sent, and they will be sent to the
     * client after.
     */
    public void enqueuePacket(PacketBuilder packet) {
        insertedPackets.add(packet.build(compressionManager));
    }

    /**
     * When the server sends the client an encryption request, this method will be called to get the server's given
     * public key and call the replacement request sender.
     * @param encoded  the encoded public key in X509
     * @param nonce    the server's verification token
     * @param serverId the server's id (not actually used)
     */
    public void setServerEncryptionRequest(byte[] encoded, byte[] nonce, String serverId) {
        attempt(() -> {
            this.nonce = nonce;
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
            ex.printStackTrace();

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
        PacketBuilder builder = new PacketBuilder(0x01);

        byte[] encoded = serverKeyPair.getPublic().getEncoded();
        builder.writeString(serverId);    // server ID
        builder.writeVarInt(encoded.length); // pub key len
        builder.writeByteArray(encoded); // pub key
        builder.writeVarInt(nonce.length); // verify token len
        builder.writeByteArray(nonce);  // verify token

        attempt(() -> streamToClient(builder.build()));
    }

    /**
     * Method to stream a given queue of bytes to the client. Whenever this is called it also checks whether we have
     * any injected packets queued to be sent to the client.
     * @param bytes the bytes to stream
     */
    public void streamToClient(ByteQueue bytes) throws IOException {
        streamTo(streamToClient, bytes, this::clientBoundEncrypt);

        // if we need to insert packets, send at most 100 at a time
        int limit = 100;
        while (!insertedPackets.isEmpty() && limit > 0) {
            limit--;
            streamTo(streamToClient, insertedPackets.remove(), this::clientBoundEncrypt);
        }

    }

    /**
     * Method to stream a queue of bytes to a given output stream. The stream will be encrypted if encryption has been
     * enabled.
     * @param stream  the stream to write to
     * @param bytes   the bytes to write
     * @param encrypt the encryption operator
     */
    private void streamTo(OutputStream stream, ByteQueue bytes, UnaryOperator<byte[]> encrypt) throws IOException {
        byte[] b = bytes.toArray();

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

            if (!Arrays.equals(decryptedToken, nonce)) {
                throw new RuntimeException("Token could not be verified!");
            }

            clientSharedSecret = cipher.doFinal(encryptedSharedSecret);

            sendReplacementEncryptionConfirmation();
        });
    }

    /**
     * For 1.19, encryption confirmation now includes verifying the client public key.
     */
    public void setClientEncryptionConfirmation(byte[] encryptedSharedSecret, byte[] salt, byte[] signature) {
        attempt(() -> {
            // verify the player's signature
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clientProfilePublicKey);
            sig.update(nonce);
            sig.update(salt);
            sig.verify(signature);

            // same as before
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
            clientSharedSecret = cipher.doFinal(encryptedSharedSecret);

            sendReplacementEncryptionConfirmationWithProfileKey();
        });
    }

    private void sendReplacementEncryptionConfirmationWithProfileKey() {
        // authenticate the client so that the remote server will accept us
        boolean client = disconnectOnError(() -> clientAuthenticator.makeRequest(generateServerHash()));
        if (!client) { return; }

        // get keys
        attempt(() -> clientAuthenticator.getClientProfileKeyPair(this));

        // verify the connecting client connection is who they claim to be
        boolean server = disconnectOnError(() -> new ServerAuthenticator(username).makeRequest(generateServerHash()));
        if (!server) { return; }

        // encryption confirmation
        attempt(() -> {
            PacketBuilder builder = new PacketBuilder(0x01);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverRealPublicKey);
            byte[] sharedSecret = cipher.doFinal(clientSharedSecret);

            builder.writeVarInt(sharedSecret.length);
            builder.writeByteArray(sharedSecret);
            builder.writeBoolean(false); // is using nonce

            byte[] salt = generateSalt();

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(clientProfileKeyPair.getPrivate());
            sig.update(nonce);
            sig.update(salt);
            byte[] signedSignature = sig.sign();

            // no length for this since its technically encoded as a long
            builder.writeByteArray(salt);
            builder.writeVarInt(signedSignature.length);
            builder.writeByteArray(signedSignature);

            streamToServer(builder.build());

            enableEncryption();
        });
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return bytes;
    }


    /**
     * This method first authenticates with the Mojang servers, then sends the server a replacement encryption
     * confirmation message. This confirmation will include the client's real shared secret but it will be encrypted
     * using the server's real public key instead of our own. After this, encryption will be enabled for all
     * future traffic.
     */
    private void sendReplacementEncryptionConfirmation() {
        // authenticate the client so that the remote server will accept us
        boolean client = disconnectOnError(() -> clientAuthenticator.makeRequest(generateServerHash()));
        if (!client) { return; }

        // verify the connecting client connection is who they claim to be
        boolean server = disconnectOnError(() -> new ServerAuthenticator(username).makeRequest(generateServerHash()));
        if (!server) { return; }

        // encryption confirmation
        attempt(() -> {
            PacketBuilder builder = new PacketBuilder(0x01);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverRealPublicKey);
            byte[] sharedSecret = cipher.doFinal(clientSharedSecret);
            byte[] verifyToken = cipher.doFinal(nonce);

            builder.writeVarInt(sharedSecret.length);
            builder.writeByteArray(sharedSecret);

            if (Config.versionReporter().isAtLeast1_19()) {
                builder.writeBoolean(true);
            }
            builder.writeVarInt(verifyToken.length);
            builder.writeByteArray(verifyToken);

            streamToServer(builder.build());

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
        this.insertedPackets.clear();
        clientAuthenticator.reset();
    }

    /**
     * Send a masked handshake packet to the client. We will intercept the real one as it has the hostname and port of
     * our proxy instead of the ones from the real server. Vanilla clients will accept this but some servers verify
     * the hostname before allowing a connection.
     * @param protocolVersion the version of the connection protocol
     * @param nextMode        the next connection mode (login or status)
     * @param hostExtension   additional text to add to the end of the host, primarily for Forge
     */
    public void sendMaskedHandshake(int protocolVersion, int nextMode, String hostExtension) {
        attempt(() -> {
            ConnectionDetails connectionDetails = Config.getConnectionDetails();
            PacketBuilder builder = new PacketBuilder(0);

            builder.writeVarInt(protocolVersion);
            builder.writeString(connectionDetails.getHost() + hostExtension);
            builder.writeShort(connectionDetails.getPortRemote());
            builder.writeVarInt(nextMode);

            devPrintFormat(
                    "Performed handshake with %s:%d, protocol version %d\n",
                    connectionDetails.getHost(),
                    connectionDetails.getPortRemote(),
                    protocolVersion
            );

            streamToServer(builder.build());
        });
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public void sendImmediately(PacketBuilder builder) {
        attempt(() -> streamToClient(builder.build(compressionManager)));
    }

    public void setClientProfilePublicKey(byte[] arr) {
        String prefix = "-----BEGIN PUBLIC KEY-----\n";
        String suffix = "\n-----END PUBLIC KEY-----\n\n";
        attempt(() -> {
            // using KeyFactory seems to give a different result
            String encoded = prefix + new String(Base64.getEncoder().encode(arr)) + suffix;
            PEMParser p = new PEMParser(new StringReader(encoded));

            clientProfilePublicKey = (RSAPublicKey) keyConverter.getPublicKey(
                (SubjectPublicKeyInfo) p.readObject()
            );
        });
    }

    public void setClientProfileSignature(byte[] readByteArray) {

    }

    public void setClientProfileKeyPair(String privateKey, String publicKey) {
        attempt(() -> {
            PEMParser privParser = new PEMParser(new StringReader(privateKey));
            PrivateKey keyPrivate = keyConverter.getPrivateKey((PrivateKeyInfo) privParser.readObject());

            PEMParser pubParser = new PEMParser(new StringReader(publicKey));
            PublicKey keyPublic = keyConverter.getPublicKey((SubjectPublicKeyInfo) pubParser.readObject());

            this.clientProfileKeyPair = new KeyPair(keyPublic, keyPrivate);

            if (!clientProfilePublicKey.equals(keyPublic)) {
                System.err.println("Provided client key does not match. This may cause issues. Restarting Minecraft might fix this.");
            }
        });
    }
}
