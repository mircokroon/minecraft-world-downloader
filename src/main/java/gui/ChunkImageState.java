package gui;

import javafx.scene.paint.Color;

public enum ChunkImageState {
    SAVED(Color.TRANSPARENT),
    UNSAVED(Color.color(1, 0, 0, .35)),
    EXTENDED(Color.color(0, 1, 0, .3)),
    OUTDATED(Color.color(.16, .16, .16, .45));

    private final Color color;

    ChunkImageState(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public static ChunkImageState isSaved(Boolean isSaved) {
        if (isSaved) {
            return SAVED;
        } else {
            return UNSAVED;
        }
    }
}
