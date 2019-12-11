package game.data;

public enum Dimension {
    OVERWORLD,
    NETHER,
    END;

    public static Dimension fromId(int id) {
        switch (id) {
            case -1: return Dimension.NETHER;
            case 1: return Dimension.END;
            default: return Dimension.OVERWORLD;
        }
    }

    public String getPath() {
        switch (this) {
            case NETHER: return "DIM-1";
            case END: return "DIM1";
            default: return "";
        }
    }
}
