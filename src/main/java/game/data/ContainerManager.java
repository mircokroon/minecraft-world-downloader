package game.data;

public class ContainerManager {
    public ContainerManager() { }

    Coordinate3D lastInteractedWith = null;

    public void lastInteractedWith(Coordinate3D coordinates) {
        lastInteractedWith = coordinates;
        System.out.println("Last interacted with: " + coordinates);
    }

}
