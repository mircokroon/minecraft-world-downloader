package gui.components;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class QuickTooltip extends Tooltip {
    public QuickTooltip() {
        this.setHideDelay(Duration.ZERO);
        this.setShowDelay(Duration.millis(100));
    }
}
