package gui;

import java.util.function.DoubleConsumer;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;

public interface ZoomBehaviour {
    double initialBlocksPerPixel = 1.0;
    double maxBlocksPerPixel = 256;
    double minBlocksPerPixel = 1.0 / 16.0;

    double zoomInMultiplier = 2;
    double zoomOutMultiplier = 1 / zoomInMultiplier;

    void onChange(DoubleConsumer setBlocksPerPixel);

    void bind(Node targetElement);

    void handle(long time);

    default void bind(Node targetElement, DoubleConsumer handleZoom) {
        targetElement.setFocusTraversable(true);
        targetElement.getParent().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.EQUALS) {
                handleZoom.accept(zoomOutMultiplier);
            } else if (e.getCode() == KeyCode.MINUS || e.getCode() == KeyCode.SUBTRACT) {
                handleZoom.accept(zoomInMultiplier);
            }
        });
        targetElement.getParent().setOnScroll(scrollEvent -> {
            handleZoom.accept(scrollEvent.getDeltaY() > 0 ? zoomOutMultiplier : zoomInMultiplier);
        });
    }

    void onTargetChange(DoubleConsumer onTargetChange);
}
