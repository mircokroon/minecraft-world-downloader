package gui;

import java.util.function.DoubleConsumer;
import javafx.animation.Interpolator;
import javafx.scene.Node;

public class SmoothZooming implements ZoomBehaviour {
    private double blocksPerPixel;
    private double oldBlocksPerPixel;
    private double newBlocksPerPixel;

    private long lastFrame = -1;
    private long scrollTime = -1;

    private final Interpolator zoomInterpolator = Interpolator.LINEAR;
    DoubleConsumer onChange;

    public SmoothZooming() {
        this.blocksPerPixel = initialBlocksPerPixel;
        this.onChange = (v) -> {};
    }

    @Override
    public void onChange(DoubleConsumer setBlocksPerPixel) {
        this.onChange = setBlocksPerPixel;
        onChange.accept(this.blocksPerPixel);
    }

    private void interpZoom(long time) {
        double duration = .2e9;
        if (newBlocksPerPixel == blocksPerPixel) {
            return;
        }
        double ratio = (time - scrollTime) / duration;
        if (ratio > 1) {
            scrollTime = -1;
            ratio = 1;

        }

        blocksPerPixel = zoomInterpolator.interpolate(oldBlocksPerPixel, newBlocksPerPixel, ratio);
        onChange.accept(blocksPerPixel);
    }

    @Override
    public void bind(Node targetElement) {
        oldBlocksPerPixel = blocksPerPixel;
        newBlocksPerPixel = blocksPerPixel;

        DoubleConsumer handleZoom = (multiplier) -> {
            // setting scrolltime marks starting time of the zoom animation
            scrollTime = lastFrame;
            oldBlocksPerPixel = blocksPerPixel;

            newBlocksPerPixel *= multiplier;

            if (newBlocksPerPixel > maxBlocksPerPixel) { newBlocksPerPixel = maxBlocksPerPixel; }
            else if (newBlocksPerPixel < minBlocksPerPixel) { newBlocksPerPixel = minBlocksPerPixel; }
        };
        bind(targetElement, handleZoom);
    }

    @Override
    public void handle(long time) {
        this.lastFrame = time;
        interpZoom(time);
    }
}
