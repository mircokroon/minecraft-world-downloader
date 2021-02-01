package packets.builder;

/**
 * Target location for chat messages sent to the client.
 */
public enum MessageTarget {
    CHAT,
    SYSTEM,
    GAMEINFO;

    byte getIdentifier() {
        switch (this) {
            case CHAT: return 0;
            case SYSTEM: return 1;
            case GAMEINFO: return 2;
        }
        return 0;
    }

}
