package gui;

import java.util.function.DoubleConsumer;
import javafx.scene.Node;

public class SnapZooming implements ZoomBehaviour {
    private double blocksPerPixel;
    private DoubleConsumer onChange;

    public SnapZooming() {
        this.blocksPerPixel = initialBlocksPerPixel;
    }

    @Override
    public void onChange(DoubleConsumer setBlocksPerPixel) {
        this.onChange = setBlocksPerPixel;
        onChange.accept(this.blocksPerPixel);
    }

    @Override
    public void bind(Node targetElement) {
        DoubleConsumer handleZoom = (multiplier) -> {
            blocksPerPixel *= multiplier;

            if (blocksPerPixel > maxBlocksPerPixel) { blocksPerPixel = maxBlocksPerPixel; }
            else if (blocksPerPixel < minBlocksPerPixel) { blocksPerPixel = minBlocksPerPixel; }

            onChange.accept(blocksPerPixel);
        };
        bind(targetElement, handleZoom);
    }

    @Override
    public void handle(long time) { }
}
