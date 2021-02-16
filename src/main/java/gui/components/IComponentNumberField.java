package gui.components;

import javafx.beans.property.StringProperty;

/**
 * Handle number fields. We can't use the existing IntegerField class as it does not exist in Java 8.
 */
public interface IComponentNumberField {
    default void onChange() {
        textProperty().addListener((observable, oldVal, newVal) -> {
            // only check when in focus so that we can change the value programmatically
            if (isFocused() && !newVal.matches("\\d*")) {
                setText(oldVal);
            }
        });
    }

    StringProperty textProperty();
    boolean isFocused();
    void setText(String v);
    String getRealText();

    default int getAsInt() {
        if (getRealText() == null || getRealText().length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(getRealText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    default void setValue(int i) {
        setText(i + "");
    }
}
