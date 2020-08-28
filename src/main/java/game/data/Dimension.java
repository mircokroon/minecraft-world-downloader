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

    /**
     * TODO: support custom dimensions?
     */
    public static Dimension fromString(String readString) {
        switch (readString) {
            case "minecraft:the_end": return END;
            case "minecraft:the_nether": return NETHER;
            default: return OVERWORLD;
        }
    }

    public String getPath() {
        switch (this) {
            case NETHER: return "DIM-1";
            case END: return "DIM1";
            default: return "";
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case OVERWORLD: return "Overworld";
            case NETHER: return "Nether";
            case END: return "End";
            default: return "Unknown";
        }
    }

}
