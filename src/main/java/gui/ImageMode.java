package gui;

public enum ImageMode {
    NORMAL(""), CAVES("caves");

    final String path;
    ImageMode(String path) {
        this.path = path;
    }


    public String path() { return path; }
}

