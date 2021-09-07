package gui;


import config.Config;
import game.data.chunk.Chunk;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

import static util.ExceptionHandling.attempt;
import static util.ExceptionHandling.attemptQuiet;

/**
 * Class to the handle the GUI.
 */
public class GuiManager extends Application {
    private static final String TITLE = "World Downloader";
    private static boolean hasErrors;
    private static ObservableList<String> messages;

    private static GuiMap chunkGraphicsHandler;
    private static Config config;
    private static GuiManager instance;

    private static String activeScene = "";
    private static GuiSettings settingController;
    private static Image icon;
    private Stage stage;

    private Stage settingsStage;

    public static void setConfig(Config config) {
        GuiManager.config = config;
    }

    public static boolean isStarted() {
        return instance != null;
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
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.hideErrorMessage();
        }
    }

    public static void setDimension(Dimension dimension) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setDimension(dimension);
        }
    }

    public static void redirectErrorOutput() {
        messages = FXCollections.observableArrayList();

        System.setErr(new PrintStream(new ByteArrayOutputStream() {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                Platform.runLater(() -> {
                    notifyNewError();

                    messages.add(this.toString());
                    this.reset();
                });
                super.write(b, off, len);
            }
        }));
    }

    private static void notifyNewError() {
        if (!GuiManager.hasErrors) {
            GuiManager.hasErrors = true;
            if (settingController != null) {
                settingController.refreshErrorTab();
            }
        }
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.showErrorMessage();
        }
    }

    public static boolean hasErrors() {
        return hasErrors;
    }

    static ObservableList<String> getMessages() {
        return messages;
    }

    public static Stage getStage() {
        if (instance.settingsStage == null) {
            return instance.stage;
        } else {
            return instance.settingsStage;
        }
    }

    public static void closeSettings() {
        if (instance.settingsStage != null) {
            instance.settingsStage.close();
            instance.settingsStage = null;
        }
    }

    private void loadSettingsInWindow() {
        if (settingsStage != null) {
            settingsStage.requestFocus();
            return;
        }

        settingsStage = new Stage();
        settingsStage.setOnCloseRequest(e -> {
            settingController = null;
            settingsStage = null;
        });
        attempt(() -> loadScene("Settings", settingsStage));

        addIcon(settingsStage);
    }

    public static void addIcon(Stage s) {
        s.getIcons().add(icon);
    }

    static void registerSettingController(GuiSettings settings) {
        GuiManager.settingController = settings;
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

        if (name.equals("Settings")) {
            stage.setTitle(TITLE + " - Settings");
        } else {
            stage.setTitle(TITLE);
        }
        stage.setScene(scene);
        stage.show();
    }

    public static void openWebLink(String text) {
        try {
            instance.openAny(text);
        } catch (Exception ex) {
            instance.openIfSupported(Desktop.Action.BROWSE, desktop -> {
                attemptQuiet(() -> desktop.browse(new URI(text)));
            });
        }
    }
    public static void openFileLink(String text) {
        try {
            instance.openAny(text);
        } catch (Exception ex) {
            instance.openIfSupported(Desktop.Action.OPEN, desktop -> {
                attemptQuiet(() -> desktop.open(new File(text)));
            });
        }
    }

    private void openIfSupported(Desktop.Action action, Consumer<Desktop> r) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(action)){
                r.accept(desktop);
            }
        }
    }

    private void openAny(String text) throws ClassNotFoundException {
        // trigger ClassNotFoundException early - we can't catch it if it's thrown by .getHostServices
        Class.forName("com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory");

        instance.getHostServices().showDocument(text);
    }

    static Config getConfig() {
        return config;
    }

    @Override
    public void start(Stage stage) throws Exception {
        // load in all font families, apparently this may fix issues with fonts on some systems
        Font.getFamilies().forEach(Font::font);

        instance = this;
        this.stage = stage;
        icon = new Image(GuiManager.class.getResourceAsStream("/ui/icon/icon.png"));
        addIcon(this.stage);

        // when in GUI mode, close the application when the main stage is closed.
        this.stage.setOnCloseRequest(e -> {
            System.exit(0);
        });

        if (config.startWithSettings()) {
            loadSceneSettings();
        } else {
            loadSceneMap();
        }
    }

    static void setGraphicsHandler(GuiMap map) {
        chunkGraphicsHandler = map;
    }

    /**
     * Bind a tooltip that shows up immediately, since we cannot use setShowDelay in Java 8.
     * Source: https://stackoverflow.com/a/36408705
     */
    public static void bindTooltip(final Node node, final Tooltip tooltip){
        node.setOnMouseMoved(event -> tooltip.show(node, event.getScreenX(), event.getScreenY() + 15));
        node.setOnMouseExited(event -> tooltip.hide());
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


    public static void setChunkState(CoordinateDim2D coord, ChunkState state) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setChunkState(coord, state);
        }
    }

    public static void outlineExistingChunks(List<CoordinateDim2D> existing) {
        if (chunkGraphicsHandler != null) {
            existing.forEach(c -> chunkGraphicsHandler.setChunkState(c, ChunkState.exists()));
        }
    }

    public static void clearChunks() {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.clearChunks();
        }
    }
}