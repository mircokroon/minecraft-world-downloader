package gui;

import config.Config;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class ChunkState {
    public final Color COLOR_EXISTS = new Color(.8, .8, .8, .2);
    public final Color COLOR_UNSAVED = new Color(1, 0, 0, .3);

    private boolean isLoaded;
    private boolean isSaved;
    private Color color;

    public ChunkState(boolean isLoaded, boolean isSaved) {
        this.isLoaded = isLoaded;
        this.isSaved = isSaved;
        computeColor();
    }

    public static ChunkState exists() {
        return new ChunkState(false, false);
    }

    public boolean isSaved() {
        return this.isSaved;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
        computeColor();
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
        computeColor();
    }

    private void computeColor() {
        if (!isLoaded) {
            this.color = COLOR_EXISTS;
            return;
        } else if (Config.markUnsavedChunks() && !isSaved) {
            this.color = COLOR_UNSAVED;
        } else {
            this.color = Color.TRANSPARENT;
        }
    }


    public Paint getColor() {
        return color;
    }

    private Color add(Color a, Color b) {
        return new Color(
                a.getRed() + b.getRed(),
                a.getGreen() + b.getGreen(),
                a.getBlue() + b.getBlue(),
                a.getOpacity()
        );
    }
}
