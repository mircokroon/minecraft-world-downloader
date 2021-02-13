package gui;


import config.Config;
import game.data.coordinates.CoordinateDim2D;
import game.data.chunk.Chunk;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import static util.ExceptionHandling.attempt;

/**
 * Class to the handle the GUI.
 */
public class GuiManager extends Application {
    private static GuiMap chunkGraphicsHandler;
    private static Config config;
    private static GuiManager instance;

    private static String activeScene = "";
    private Stage stage;

    private Stage settingsStage;


    public static void setConfig(Config config) {
        GuiManager.config = config;
    }

    public static void loadSceneMap() {
        activeScene = "Map";
        loadSceneOrLaunch();
    }

    public static void loadSceneSettings() {
        activeScene = "Settings";
        loadSceneOrLaunch();
    }

    public static void loadWindowSettings() {
        instance.loadSettingsInWindow();
    }

    private void loadSettingsInWindow() {
        if (settingsStage != null) {
            settingsStage.requestFocus();
            return;
        }

        settingsStage = new Stage();
        settingsStage.setOnCloseRequest(e -> settingsStage = null);
        attempt(() -> loadScene("Settings", settingsStage));
    }

    private static void loadSceneOrLaunch() {
        if (instance == null) {
            new Thread(Application::launch).start();
        } else {
            try {
                instance.loadScene(activeScene, instance.stage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void loadScene(String name, Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/ui/" + name + ".fxml"));

        Scene scene = new Scene(root);

        stage.setTitle("World Downloader");
        stage.setScene(scene);
        stage.show();
    }

    static Config getConfig() {
        return config;
    }

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        this.stage = stage;

        if (config.isValid()) {
            loadSceneMap();
        } else {
            loadSceneSettings();
        }
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

