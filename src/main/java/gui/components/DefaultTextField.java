package gui.components;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.control.TextField;

/**
 * Text field with a default value. When the field has the value of the default, it will be shown as "default" and
 * greyed out.
 */
public class DefaultTextField extends TextField {
    private static final String defaultPlaceholder = "default";

    private final StringProperty defaultVal = new SimpleStringProperty();
    private PseudoClass isDefaultPseudoClass;
    private boolean isDefault = false;


    public DefaultTextField() {
        Platform.runLater(this::setupDefaultSwapping);
    }

    public DefaultTextField(String s) {
        super(s);
        Platform.runLater(this::setupDefaultSwapping);
    }

    /**
     * Set up the content swapping behaviour.
     */
    private void setupDefaultSwapping() {
        isDefaultPseudoClass = PseudoClass.getPseudoClass("is-default");

        handleFieldDefault();

        focusedProperty().addListener((ov, oldV, inFocus) -> {
            if (inFocus && getText().equals(defaultPlaceholder)) {
                setText(getDefaultVal());
                pseudoClassStateChanged(isDefaultPseudoClass, false);
                isDefault = false;
            }

            if (!inFocus) {
                handleFieldDefault();
            }
        });
    }

    /**
     * Check if the field has the default value, and act accordingly.
     */
    private void handleFieldDefault() {
        if (getText().equals(getDefaultVal())) {
            setText(defaultPlaceholder);
            pseudoClassStateChanged(isDefaultPseudoClass, true);
            isDefault = true;
        }
    }


    public final String getDefaultVal() { return defaultVal.get(); }
    public final void setDefaultVal(String foo) { this.defaultVal.set(foo);}

    public String getRealText() {
        if (isDefault) {
            return getDefaultVal();
        } else {
            return getText();
        }
    }
}
