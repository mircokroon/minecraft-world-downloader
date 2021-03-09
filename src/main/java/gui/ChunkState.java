package gui;

import config.Config;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class ChunkState {
    public final Color COLOR_EXISTS = new Color(.8, .8, .8, .2);
    public final Color COLOR_UNSAVED = new Color(1, 0, 0, .3);
    public final Color COLOR_LIT = new Color(0, 1, 0, .3);

    private boolean isLoaded;
    private boolean isSaved;
    private boolean isLit;
    private Color color;

    public ChunkState(boolean isLoaded, boolean isSaved, boolean isLit) {
        this.isLoaded = isLoaded;
        this.isSaved = isSaved;
        this.isLit = isLit;
        computeColor();
    }

    public static ChunkState exists() {
        return new ChunkState(false, false, false);
    }

    public boolean isSaved() {
        return this.isSaved;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public boolean isLit() {
        return isLit;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
        computeColor();
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
        computeColor();
    }

    public void setLit(boolean lit) {
        isLit = lit;
        computeColor();
    }

    private void computeColor() {
        Color col = Color.TRANSPARENT;

        if (!isLoaded) {
            this.color = COLOR_EXISTS;
            return;
        }

        if (Config.markUnsavedChunks() && !isSaved) {
            col = add(COLOR_UNSAVED, col);
        }
        if (Config.isInDevMode() && isLit) {
            col = add(COLOR_LIT, col);
        }
        this.color = col;
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
