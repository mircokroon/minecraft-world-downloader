package proxy;

import game.Game;
import packets.ClientBoundLoginPacketBuilder;
import packets.ServerBoundLoginPacketBuilder;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {
    public final int blockSize = 16;
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    boolean encryptionEnabled = false;

    public int getCompressionLimit() {
        return compressionLimit;
    }

    public void setCompressionLimit(int compressionLimit) {
        // TODO: enable compression
        this.compressionLimit = compressionLimit;
    }

    int compressionLimit = -1;

    private Cipher clientBoundDecryptor, clientBoundEncryptor, serverBoundEncryptor, serverBoundDecryptor;

    private OutputStream streamToClient;
    private OutputStream streamToServer;

    String serverId;
    RSAPublicKey serverRealPublicKey;
    byte[] serverVerifyToken;

    byte[] clientSharedSecret;

    private KeyPair serverKeyPair;
    {
        attempt(() -> {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            serverKeyPair = keyGen.generateKeyPair();
        });
    }

    public void setServerEncryptionRequest(byte[] encoded, byte[] token, String serverId) {
        attempt(() -> {
            serverVerifyToken = token;
            this.serverId = serverId;

            KeyFactory kf = KeyFactory.getInstance("RSA");
            serverRealPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));

            sendReplacementEncryptionRequest();
        });

    }


    public void sendReplacementEncryptionRequest() {
        List<Byte> bytes = new ArrayList<>();
        byte[] encoded = serverKeyPair.getPublic().getEncoded();
        writeVarInt(bytes, ClientBoundLoginPacketBuilder.ENCRYPTION_REQUEST);   // packet ID
        writeString(bytes, "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0");    // server ID
        writeVarInt(bytes, encoded.length); // pub key len
        writeByteArray(bytes, encoded); // pub key
        writeVarInt(bytes, serverVerifyToken.length); // verify token len
        writeByteArray(bytes, serverVerifyToken);  // verify token
        prependPacketLength(bytes);

        attempt(() -> streamToClient(new LinkedList<>(bytes)));
    }

    public void setClientEncryptionConfirmation(byte[] sharedSecret, byte[] token) {
        attempt(() -> {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, serverKeyPair.getPrivate());
            byte[] decryptedToken = cipher.doFinal(token);

            if (!Arrays.equals(decryptedToken, serverVerifyToken)) {
                throw new RuntimeException("Token could not be verified!");
            }

            clientSharedSecret = cipher.doFinal(sharedSecret);

            sendReplacementEncryptionConfirmation();
        });
    }

    private void sendReplacementEncryptionConfirmation() {
        // authenticate the client
        attempt(() -> new ClientAuthenticator().makeRequest(generateShaHash()));

        // encryption confirmation
        attempt(() -> {
            List<Byte> bytes = new ArrayList<>();

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverRealPublicKey);
            byte[] sharedSecret = cipher.doFinal(clientSharedSecret);
            byte[] verifyToken = cipher.doFinal(serverVerifyToken);

            writeVarInt(bytes, ServerBoundLoginPacketBuilder.ENCRYPTION_RESPONSE);
            writeVarInt(bytes, sharedSecret.length);
            writeByteArray(bytes, sharedSecret);
            writeVarInt(bytes, verifyToken.length);
            writeByteArray(bytes, verifyToken);
            prependPacketLength(bytes);

            streamToServer(new LinkedList<>(bytes));

            enableEncryption();
        });
    }

    private void enableEncryption() {
        attempt(() -> {
            IvParameterSpec ivspec = new IvParameterSpec(clientSharedSecret);
            SecretKeySpec k = new SecretKeySpec(clientSharedSecret, "AES");
            clientBoundEncryptor = Cipher.getInstance("AES/CFB8/PKCS5Padding");
            clientBoundEncryptor.init(Cipher.ENCRYPT_MODE, k, ivspec);

            clientBoundDecryptor = Cipher.getInstance("AES/CFB8/PKCS5Padding");
            clientBoundDecryptor.init(Cipher.DECRYPT_MODE, k, ivspec);

            serverBoundEncryptor = Cipher.getInstance("AES/CFB8/PKCS5Padding");
            serverBoundEncryptor.init(Cipher.ENCRYPT_MODE, k, ivspec);

            serverBoundDecryptor = Cipher.getInstance("AES/CFB8/PKCS5Padding");
            serverBoundDecryptor.init(Cipher.DECRYPT_MODE, k, ivspec);

            encryptionEnabled = true;
            System.out.println("Enabled encryption");
        });
    }

    private String generateShaHash() {
        AtomicReference<MessageDigest> sha1 = new AtomicReference<>();
        attempt(() -> sha1.set(MessageDigest.getInstance("SHA1")));

        sha1.get().update(serverId.getBytes(StandardCharsets.US_ASCII));
        sha1.get().update(clientSharedSecret);
        sha1.get().update(serverRealPublicKey.getEncoded());

        return new BigInteger(sha1.get().digest()).toString(16);
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

    public byte[] clientBoundDecrypt(byte[] bytes) {
        return decrypt(bytes, clientBoundDecryptor);
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

    private byte[] encrypt(byte[] bytes, Cipher encryptor) {
        if (!encryptionEnabled) { return bytes; }

        try {
            return encryptor.update(bytes);
        } catch(Exception ex) {
            throw new RuntimeException("Could not encrypt stream!", ex);
        }
    }

    private byte[] serverBoundEncrypt(byte[] bytes) {
        return encrypt(bytes, serverBoundEncryptor);
    }

    private byte[] clientBoundEncrypt(byte[] bytes) {
        return encrypt(bytes, clientBoundEncryptor);
    }

    public void streamToServer(Queue<Byte> bytes) throws IOException {
        //System.out.println("Writing bytes to server: " + bytes.size() + " :: " + bytes);
        streamTo(streamToServer, bytes, this::serverBoundEncrypt);
    }
    public void streamToClient(Queue<Byte> bytes) throws IOException {
        //System.out.println("Writing bytes to client: " + bytes.size() + " :: " + bytes);
        streamTo(streamToClient, bytes, this::clientBoundEncrypt);
    }

    private void streamTo(OutputStream stream, Queue<Byte> bytes, UnaryOperator<byte[]> encrypt) throws IOException {
        byte[] b = new byte[bytes.size()];
        for (int i = 0; i < b.length; i++) {
            b[i] = bytes.remove();
        }

        byte[] encrypted = encrypt.apply(b);

        stream.write(encrypted, 0, encrypted.length);
        stream.flush();
    }

    public static void attempt(IExceptionHandler r) {
        try {
            r.run();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Encryption failure! Terminating.");
            System.exit(1);
        }
    }

    public static void prependPacketLength(List<Byte> bytes) {
        int len = bytes.size();

        List<Byte> varIntLen = new ArrayList<>(5);
        writeVarInt(varIntLen, len);
        bytes.addAll(0, varIntLen);
    }

    public static void writeByteArray(List<Byte> list, byte[] bytes) {
        for (byte b : bytes) {
            list.add(b);
        }
    }

    public static void writeString(List<Byte> bytes, String str) {
        final byte[][] stringBytes = {null};
        attempt(() -> stringBytes[0] = str.getBytes("UTF-8"));
        writeVarInt(bytes, stringBytes[0].length);
        writeByteArray(bytes, stringBytes[0]);
    }

    public static void writeVarInt(List<Byte> bytes, int value) {
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            bytes.add(temp);
        } while (value != 0);
    }

    public void reset() {
        encryptionEnabled = false;
    }

    public void sendMaskedHandshake(int protocolVersion, int nextMode) {
        attempt(() -> {
            List<Byte> bytes = new ArrayList<>();

            writeVarInt(bytes, 0);
            writeVarInt(bytes, protocolVersion);
            writeString(bytes, Game.getHost());
            writeShort(bytes, Game.getPortRemote());
            writeVarInt(bytes, nextMode);
            prependPacketLength(bytes);

            streamToServer(new LinkedList<>(bytes));
            System.out.println("Sent masked handshake with " + Game.getHost() + ":" + Game.getPortRemote());
        });
    }

    private static void writeShort(List<Byte> bytes, int portRemote) {
        bytes.add((byte) ((portRemote >>> 8) & 0xFF));
        bytes.add((byte) ((portRemote) & 0xFF));
    }

}
