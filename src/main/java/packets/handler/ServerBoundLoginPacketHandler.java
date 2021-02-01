package packets.handler;

import game.Game;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundLoginPacketHandler extends PacketHandler {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundLoginPacketHandler() {

        operations.put("login_start", provider -> {
            String username = provider.readString();
            System.out.println("Login by: " + username);

            Game.getEncryptionManager().setUsername(username);
            return true;
        });

        operations.put("encryption_response", provider -> {
            int sharedSecretLength = provider.readVarInt();
            byte[] sharedSecret = provider.readByteArray(sharedSecretLength);
            int verifyTokenLength = provider.readVarInt();
            byte[] verifyToken = provider.readByteArray(verifyTokenLength);

            Game.getEncryptionManager().setClientEncryptionConfirmation(sharedSecret, verifyToken);
            return false;
        });
    }

    @Override
    public Map<String, PacketOperator> getOperators() {
        return operations;
    }

    @Override
    public boolean isClientBound() {
        return false;
    }
}
