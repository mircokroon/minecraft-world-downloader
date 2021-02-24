package game.data.entity;

import com.google.gson.Gson;
import game.data.coordinates.CoordinateDouble3D;
import kong.unirest.Unirest;
import packets.DataTypeProvider;
import packets.UUID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEntity implements IMovableEntity {
    final static Map<UUID, String> knownNames = new ConcurrentHashMap<>();
    final static String API_GET_NAME = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private CoordinateDouble3D pos;
    private UUID uuid;
    private boolean hasRequestedName = false;
    private String name;

    public static PlayerEntity parse(DataTypeProvider provider) {
        PlayerEntity ent = new PlayerEntity();
        ent.uuid = provider.readUUID();
        ent.readPosition(provider);

        return ent;
    }

    /**
     * Fetch the player's name from the Mojang API.
     */
    private void fetchName() {
        if (hasRequestedName) {
            return;
        }
        hasRequestedName = true;

        if (knownNames.containsKey(uuid)) {
            System.out.println("Retrieved from map: " + knownNames.get(uuid));
            this.name = knownNames.get(uuid);
            return;
        }

        Unirest.get(API_GET_NAME + uuid.toString()).asStringAsync((str) -> {
            if (!str.isSuccess()) {
                return;
            }

            PlayerNameResponse res = new Gson().fromJson(str.getBody(), PlayerNameResponse.class);
            knownNames.put(uuid, res.name);
            this.name = res.name;
        });
    }

    static class PlayerNameResponse {
        String name;
    }

    @Override
    public void incrementPosition(int dx, int dy, int dz) {
        pos.increment(
                dx / Entity.CHANGE_MULTIPLIER,
                dy / Entity.CHANGE_MULTIPLIER,
                dz / Entity.CHANGE_MULTIPLIER
        );
    }

    @Override
    public void readPosition(DataTypeProvider provider) {
        this.pos = provider.readDoubleCoordinates();
    }

    @Override
    public String toString() {
        return "PlayerEntity{" +
                "uuid=" + uuid +
                '}';
    }

    public CoordinateDouble3D getPosition() {
        return pos;
    }

    public String getName() {
        if (!hasRequestedName) {
            fetchName();
        }
        return name;
    }
}
