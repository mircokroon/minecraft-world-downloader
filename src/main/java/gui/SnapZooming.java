package gui;

import java.util.function.DoubleConsumer;
import javafx.scene.Node;

public class SnapZooming implements ZoomBehaviour {
    private double blocksPerPixel;
    private DoubleConsumer onChange, onTargetChange;

    public SnapZooming() {
        this.blocksPerPixel = initialBlocksPerPixel;
    }

    @Override
    public void onChange(DoubleConsumer setBlocksPerPixel) {
        this.onChange = setBlocksPerPixel;
        onChange.accept(this.blocksPerPixel);
    }

    @Override
    public void onTargetChange(DoubleConsumer onTargetChange) {
        this.onTargetChange = onTargetChange;
        onTargetChange.accept(this.blocksPerPixel);
    }

    @Override
    public void bind(Node targetElement) {
        DoubleConsumer handleZoom = (multiplier) -> {
            blocksPerPixel *= multiplier;

            if (blocksPerPixel > maxBlocksPerPixel) { blocksPerPixel = maxBlocksPerPixel; }
            else if (blocksPerPixel < minBlocksPerPixel) { blocksPerPixel = minBlocksPerPixel; }

            onTargetChange.accept(blocksPerPixel);
            onChange.accept(blocksPerPixel);
        };
        bind(targetElement, handleZoom);
    }

    @Override
    public void handle(long time) { }
}
