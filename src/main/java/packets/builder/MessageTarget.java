package packets.builder;

/**
 * Target location for chat messages sent to the client.
 */
public enum MessageTarget {
    CHAT,
    SYSTEM,
    GAMEINFO;

    byte getIdentifier() {
        return switch (this) {
            case CHAT -> 0;
            case SYSTEM -> 1;
            case GAMEINFO -> 2;
        };
    }

}
