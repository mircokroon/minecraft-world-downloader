package gui.components;

import javafx.scene.control.TextField;

/**
 * Integer field class. Most of the functionality is in the interface.
 */
public class IntField extends TextField implements IComponentNumberField {
    public IntField() {
        onChange();
    }

    public IntField(String s) {
        super(s);
        onChange();
    }

    @Override
    public String getRealText() {
        return getText();
    }
}
