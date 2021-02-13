package gui;

import javafx.scene.image.Image;

public class ChunkImage {
    Image image;
    boolean isSaved;

    public ChunkImage(Image image, boolean isSaved) {
        this.image = image;
        this.isSaved = isSaved;
    }

    public Image getImage() {
        return image;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }
}
