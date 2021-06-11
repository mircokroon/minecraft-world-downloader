package game.data.region;

/**
 * Class to hold the combination of entity and region MCA files for.
 */
public class McaFilePair {
    McaFile region;
    McaFile entities;

    public McaFilePair(McaFile region, McaFile entities) {
        this.region = region;
        this.entities = entities;
    }

    public McaFile getRegion() {
        return region;
    }

    public McaFile getEntities() {
        return entities;
    }
}
