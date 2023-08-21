package gui.images;

public enum ImageMode {
    NORMAL(""), CAVES("caves");

    final String path;
    ImageMode(String path) {
        this.path = path;
    }

    public ImageMode other() {
        if (this == NORMAL) {
            return CAVES;
        }
        return NORMAL;
    }

    public String path() { return path; }
}

