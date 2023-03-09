package gui;

import javafx.animation.Interpolator;

/**
 * Double that changes smoothly over time when set to a new value.
 */
public class InterpolatedDouble {
    private final Interpolator zoomInterpolator = Interpolator.LINEAR;

    private final double duration;
    private double oldVal, newVal, value;
    long lastUpdated = -1;
    long currentTime = -1;

    public InterpolatedDouble(double duration, double value) {
        this.duration = duration;

        this.value = value;
        this.oldVal = value;
        this.newVal = value;
    }

    public boolean interp(long time) {
        this.currentTime = time;

        if (newVal == value) {
            return false;
        }
        double ratio = (time - lastUpdated) / duration;
        if (ratio > 1) {
            lastUpdated = -1;
            ratio = 1;
        }

        value = zoomInterpolator.interpolate(oldVal, newVal, ratio);
        return true;
    }

    public void setTargetValue(double newVal) {
        lastUpdated = currentTime;

        this.newVal = newVal;
        this.oldVal = this.value;
    }

    public double getCurrentValue() {
        return value;
    }

    public double getTargetValue() {
        return newVal;
    }
}
