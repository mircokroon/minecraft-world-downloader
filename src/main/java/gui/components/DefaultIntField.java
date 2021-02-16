package gui.components;

/**
 * Integer field with a default value. When the field has the value of the default, it will be shown as "default" and
 * greyed out.
 */
public class DefaultIntField extends DefaultTextField implements IComponentNumberField {
    public DefaultIntField() {
        onChange();
    }

    public DefaultIntField(String s) {
        super(s);
        onChange();
    }
}
