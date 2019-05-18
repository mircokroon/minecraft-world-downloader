package packets.builder;

import game.Game;
import game.data.Coordinate3D;
import packets.DataTypeProvider;

import java.util.HashMap;
import java.util.Map;

public class ServerBoundLoginPacketBuilder extends PacketBuilder {
    private HashMap<String, PacketOperator> operations = new HashMap<>();
    public ServerBoundLoginPacketBuilder() {

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
