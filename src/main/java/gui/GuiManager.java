package gui;


import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.Chunk;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

/**
 * Class to the handle the GUI.
 */
public class GuiManager extends Application {
    private static GuiMap chunkGraphicsHandler;

    private static boolean startSettings;
    public static void showGUI(boolean startSettings) {
        GuiManager.startSettings = startSettings;
        new Thread(Application::launch).start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root;
        if (startSettings) {
            root = FXMLLoader.load(getClass().getResource("/ui/Settings.fxml"));
        } else {
            root = FXMLLoader.load(getClass().getResource("/ui/Map.fxml"));
        }

        Scene scene = new Scene(root);

        stage.setTitle("World Downloader");
        stage.setScene(scene);
        stage.show();
    }

    static void setGraphicsHandler(GuiMap map) {
        chunkGraphicsHandler = map;
    }


    /**
     * Set a chunk to being loaded.
     * @param coord the chunk coordinates
     * @param chunk the chunk object
     */
    public static void setChunkLoaded(CoordinateDim2D coord, Chunk chunk) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setChunkLoaded(coord, chunk);
        }
    }

    public static void markChunkSaved(CoordinateDim2D coord) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.markChunkSaved(coord);
        }
    }

    public static void outlineExistingChunks(List<CoordinateDim2D> existing) {
        if (chunkGraphicsHandler != null) {
            existing.forEach(chunkGraphicsHandler::setChunkExists);
        }
    }

    public static void clearChunks() {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.clearChunks();
        }
    }
}

