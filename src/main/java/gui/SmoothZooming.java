package gui;

import java.util.function.DoubleConsumer;
import javafx.scene.Node;

public class SmoothZooming implements ZoomBehaviour {
    private final InterpolatedDouble blocksPerPixel;

    DoubleConsumer onChange, onTargetChange;

    public SmoothZooming() {
        this.blocksPerPixel = new InterpolatedDouble(.2e9, 1.0);
        this.onChange = (v) -> {};
    }

    @Override
    public void onTargetChange(DoubleConsumer onTargetChange) {
        this.onTargetChange = onTargetChange;
        onTargetChange.accept(this.blocksPerPixel.getCurrentValue());
    }

    @Override
    public void onChange(DoubleConsumer setBlocksPerPixel) {
        this.onChange = setBlocksPerPixel;
        onChange.accept(this.blocksPerPixel.getCurrentValue());
    }

    @Override
    public void bind(Node targetElement) {
        DoubleConsumer handleZoom = (multiplier) -> {
            double targetVal = blocksPerPixel.getTargetValue() * multiplier;

            targetVal = Math.max(minBlocksPerPixel, Math.min(targetVal, maxBlocksPerPixel));

            onTargetChange.accept(targetVal);
            blocksPerPixel.setTargetValue(targetVal);
        };
        bind(targetElement, handleZoom);
    }

    @Override
    public void handle(long time) {
        if (this.blocksPerPixel.interp(time)) {
            onChange.accept(blocksPerPixel.getCurrentValue());
        }
    }
}
