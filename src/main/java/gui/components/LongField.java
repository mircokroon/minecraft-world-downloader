package gui.components;

/**
 * Long field class.
 */
public class LongField extends IntField {
    public long getAsLong() {
        if (getRealText() == null || getRealText().length() == 0) {
            return 0;
        }

        try {
            return Long.parseLong(getRealText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public void setLongValue(long levelSeed) {
        setText(levelSeed + "");
    }
}
