package gui;

import config.Config;
import java.util.function.BooleanSupplier;
import javafx.scene.paint.Color;

public enum ChunkImageState {
    SAVED(Color.TRANSPARENT),
    DEBUG(Color.color(0, 0, 1, .3)),
    UNSAVED(Color.color(1, 0, 0, .35), Config::markUnsavedChunks),
    EXTENDED(Color.color(0, 1, 0, .3), Config::drawExtendedChunks),
    OUTDATED(Color.color(.16, .16, .16, .45), Config::markOldChunks);

    private final Color color;
    private final BooleanSupplier condition;

    ChunkImageState(Color color, BooleanSupplier condition) {
        this.color = color;
        this.condition = condition;
    }
    ChunkImageState(Color color) {
        this(color, () -> true);
    }


    public Color getColor() {
        if (!condition.getAsBoolean()) {
            return Color.TRANSPARENT;
        }
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
