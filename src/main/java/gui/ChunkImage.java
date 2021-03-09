package gui;

import javafx.scene.image.Image;

public class ChunkImage {
    Image image;
    ChunkState state;

    public ChunkImage(Image image, ChunkState state) {
        this.image = image;
        this.state = state;
    }

    public Image getImage() {
        return image;
    }

    public boolean isSaved() {
        return state.isSaved();
    }

    public void setState(ChunkState state) {
        this.state = state;
    }

    public ChunkState getState() {
        return state;
    }
}
