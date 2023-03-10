package gui;


import config.Config;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URI;

import static util.ExceptionHandling.attempt;

/**
 * Class to the handle the GUI.
 */
public class GuiManager extends Application {
    private static final String TITLE = "World Downloader";
    private static boolean hasErrors;
    private static boolean authenticationFailed;
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

    public static void setStatusMessage(String str) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setStatusMessage(str);
        }
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

    public static boolean clearAuthentiationStatus() {
        return authenticationFailed;
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

    public static void resetRegion(Coordinate2D regionLocation) {
        chunkGraphicsHandler.getRegionHandler().resetRegion(regionLocation);
    }

    public static void setAuthenticationFailed() {
        authenticationFailed = true;
    }

    public static void clearAuthenticationFailed() {
        authenticationFailed = false;
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

    private <T> T loadScene(String name, Stage stage) throws IOException {
        return loadScene(name, stage, null);
    }

    private <T> T loadScene(String name, Stage stage, Class<T> controllerType) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/" + name + ".fxml"));


        Scene scene = new Scene(loader.load());

        if (name.equals("Settings")) {
            stage.setTitle(TITLE + " - Settings");
        } else if (name.equals("MicrosoftAuth")) {
            stage.setTitle(TITLE + " - Microsoft Authentication");
        } else {
            stage.setTitle(TITLE);
        }
        stage.setScene(scene);
        stage.show();

        if (controllerType != null) {
            return controllerType.cast(loader.getController());
        }

        return null;
    }

    public static boolean openWebLink(String text) {
        try {
            instance.openAny(text);
        } catch (Exception ex) {
            try {
                instance.openIfSupported(Desktop.Action.BROWSE, desktop -> {
                    desktop.browse(new URI(text));
                });
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
    public static boolean openFileLink(String text) {
        try {
            instance.openAny(text);
        } catch (Exception ex) {
            try {
                instance.openIfSupported(Desktop.Action.OPEN, desktop -> {
                    desktop.open(new File(text));
                });
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    interface DesktopConsumer {
        void accept(Desktop desktop) throws IOException, URISyntaxException;
    }

    private void openIfSupported(Desktop.Action action, DesktopConsumer r) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(action)){
                try {
                    r.accept(desktop);
                } catch (IOException|URISyntaxException e) {
                    throw new UnsupportedOperationException(e);
                }
                return;
            }
        }
        throw new UnsupportedOperationException("Cannot perform this action: " + action);
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
        Config.handleErrorOutput();

        // load in all font families, apparently this may fix issues with fonts on some systems
        Font.getFamilies().forEach(Font::font);

        instance = this;
        this.stage = stage;
        icon = new Image(GuiManager.class.getResourceAsStream("/ui/icon/icon.png"));
        addIcon(this.stage);

        // when in GUI mode, close the application when the main stage is closed.
        this.stage.setOnCloseRequest(e -> {
            saveAndExit();
        });

        if (config.startWithSettings()) {
            loadSceneSettings();
        } else {
            loadSceneMap();
        }
    }

    public static void saveAndExit() {
        getStage().hide();

        Platform.runLater(() -> {
            // first stop both saving executor threads
            chunkGraphicsHandler.getRegionHandler().shutdown();
            WorldManager.getInstance().shutdown();

            // then save world, if they are already in the process of saving they will wait for the
            // executor to finish before returning
            chunkGraphicsHandler.getRegionHandler().save();
            WorldManager.getInstance().save();

            Platform.exit();
            System.exit(0);
        });
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

    public static void clearChunks() {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.clearChunks();
        }
    }
}