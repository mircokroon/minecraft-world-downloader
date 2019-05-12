package game;

public enum NetworkMode {
    HANDSHAKE,
    STATUS,
    LOGIN,
    GAME;

    @Override
    public String toString() {
        switch (this) {
            case HANDSHAKE:
                return "HANDSHAKE";
            case STATUS:
                return "STATUS";
            case LOGIN:
                return "LOGIN";
            case GAME:
                return "GAME";
        }
        return "";
    }
}
