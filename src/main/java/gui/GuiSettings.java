package gui;


import static util.ExceptionHandling.attemptQuiet;

import config.Config;
import gui.components.DefaultIntField;
import gui.components.IntField;
import gui.components.LongField;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import util.PathUtils;

public class GuiSettings {
    public TextField server;
    public DefaultIntField portLocal;

    public TextField worldOutputDir;
    public IntField centerX;
    public IntField centerZ;
    public LongField levelSeed;

    public CheckBox markUnsaved;
    public CheckBox markOld;

    public Button saveButton;
    public Tab errTab;
    public TabPane tabPane;
    public TextArea errOutput;
    public CheckBox disableWorldGen;
    public Label portVerifyLabel;
    public Slider extendedDistance;
    public IntField extendedDistanceText;
    public Hyperlink openWorldDir;
    public Hyperlink verifyAuthLink;
    public CheckBox renderOtherPlayers;
    public CheckBox enableInfoMessages;
    public Tab generalTab;
    public Tab authTab;
    public Tab realmsTab;
    public RealmsTabController realmsController;
    public AuthTabController authController;
    public CheckBox enableDrawExtendedChunks;
    public CheckBox enableCaveRenderMode;

    Config config;
    private boolean portInUse;

    Map<Tab, Integer> heights;

    public GuiSettings() {
        this.config = GuiManager.getConfig();
        GuiManager.getStage().setResizable(false);
        GuiManager.registerSettingController(this);
    }

    @FXML
    void initialize() {
        if (config.isStarted()) {
            saveButton.setText("Save");
        }

        heights = Map.of(
            generalTab, 360,
            realmsTab, 320
        );

        // connection tab
        server.setText(config.server);
        portLocal.setText("" + config.portLocal);

        // output tab
        worldOutputDir.setText(config.worldOutputDir);
        centerX.setValue(config.centerX);
        centerZ.setValue(config.centerZ);
        levelSeed.setLongValue(config.levelSeed);
        disableWorldGen.setSelected(config.disableWorldGen);

        // general tab
        extendedDistance.setValue(config.extendedRenderDistance);
        extendedDistanceText.setValue(config.extendedRenderDistance);
        markUnsaved.setSelected(!config.disableMarkUnsavedChunks);
        markOld.setSelected(config.markOldChunks);
        renderOtherPlayers.setSelected(config.renderOtherPlayers);
        enableInfoMessages.setSelected(!config.disableInfoMessages);
        enableCaveRenderMode.setSelected(config.enableCaveRenderMode);
        enableDrawExtendedChunks.setSelected(config.drawExtendedChunks);

        // realms tab
        if (config.isStarted()) {
            tabPane.getTabs().remove(realmsTab);
        } else {
            tabPane.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
                save();
                if (newVal == realmsTab) {
                    realmsController.opened(this);
                }
                if (newVal == authTab) {
                    authController.opened(this);
                }

                if (heights.containsKey(newVal)) {
                    GuiManager.getStage().setHeight(heights.get(newVal));
                } else {
                    resetHeight();
                }
            });
        }
        disableWhenRunning(Arrays.asList(server, portLocal, centerX, centerZ, worldOutputDir));

        GuiManager.bindTooltip(portVerifyLabel, new Tooltip("Is the downloader already running?"));

        openWorldDir.setOnAction(e -> attemptQuiet(() -> {
            Path p = PathUtils.toPath(worldOutputDir.getText());
            File f = p.toFile();
            if (f.exists() && f.isDirectory()) {
                GuiManager.openFileLink(p.toString());
            } else if (p.getParent().toFile().exists()) {
                GuiManager.openFileLink(p.getParent().toString());
            }
        }));

        verifyAuthLink.setOnAction(e -> tabPane.getSelectionModel().select(authTab));

        handleDataValidation();
        handleErrorTab();
        handleResizing();

        resetHeight();
    }

    private void resetHeight() {
        GuiManager.getStage().setHeight(290);
    }

    private void handleDataValidation() {
        // disable button when field is empty
        server.textProperty().addListener((ov, oldV, newV) -> {
            updateSaveButtonState();
        });

        // register empty pseudoclass to color field when its empty
        PseudoClass empty = PseudoClass.getPseudoClass("empty");
        server.textProperty().addListener((obs, oldText, newText) -> {
            server.pseudoClassStateChanged(empty, newText.isEmpty());
        });
        server.pseudoClassStateChanged(empty, server.getText() == null || server.getText().isEmpty());

        // verify port on focus loss
        portLocal.focusedProperty().addListener((ov, oldVal, newVal) -> verifyLocalPort());
        verifyLocalPort();

        extendedDistance.valueProperty().addListener((ov, oldV, newV) -> setRenderDistance(newV.intValue()));
        extendedDistanceText.textProperty().addListener((ov, oldV, newV) -> {
            if (newV.length() > 0) {
                setRenderDistance(extendedDistanceText.getAsInt());
            }
        });
    }

    private void setRenderDistance(int v) {
        int val = Math.max(0, Math.min(32, v));
        extendedDistance.setValue(val);
        extendedDistanceText.setValue(val);
    }

    private void updateSaveButtonState() {
        int len = server.getText() == null ? 0 : server.getText().length();
        saveButton.setDisable(len == 0 || portInUse);
    }


    private void handleResizing() {
        // make sure the error output resizes with the window
        Platform.runLater(() -> {
            GuiManager.getStage().heightProperty().addListener((ov, oldVal, newVal) -> {
                double delta = newVal.doubleValue() - oldVal.doubleValue();
                tabPane.setPrefHeight(tabPane.getPrefHeight() + delta);
                saveButton.setLayoutY(saveButton.getLayoutY() + delta);
            });

            GuiManager.getStage().widthProperty().addListener((ov, oldVal, newVal) -> {
                double delta = newVal.doubleValue() - oldVal.doubleValue();
                tabPane.setPrefWidth(tabPane.getPrefWidth() + delta);
                saveButton.setLayoutX(saveButton.getLayoutX() + delta);
            });
        });
    }

    private void verifyLocalPort() {
        if (portInUse(portLocal.getAsInt())) {
            portVerifyLabel.setText("Port in use!");
            portInUse = true;
        } else {
            portVerifyLabel.setText("");
            portInUse = false;
        }
        updateSaveButtonState();
    }

    public void refreshErrorTab() {
        tabPane.getTabs().add(errTab);
        handleErrorTab();
    }

    private void handleErrorTab() {
        // remove error tag if there is no errors
        if (!GuiManager.hasErrors()) {
            tabPane.getTabs().remove(errTab);
            return;
        } else if (GuiManager.clearAuthentiationStatus()) {
            tabPane.getSelectionModel().select(authTab);
        } else {
            tabPane.getSelectionModel().select(errTab);
        }
        errOutput.setWrapText(true);

        ObservableList<String> messages = GuiManager.getMessages();
        errOutput.setText(join(messages));

        try {

            GuiManager.getMessages().addListener((ListChangeListener<String>) change -> {
                errOutput.setText(join(messages));
            });
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private String join(ObservableList<String> list) {
        return list.stream().reduce((a, b) -> {
            if (a.length() == 0 || a.equals("\n")) {
                return b;
            } else if (b.length() == 0 || b.equals("\n")) {
                return a;
            } else {
                return a + "\n " + b;
            }
        }).orElse("");
    }

    /**
     * Disabled fields that should not be changed while the proxy server is already started.
     */
    private void disableWhenRunning(List<Control> controls) {
        if (!config.isStarted()) {
            return;
        }
        controls.forEach(field -> field.setDisable(true));
    }

    /**
     * Write the settings from the GUI to the config file.
     */
    public void saveSettings(ActionEvent actionEvent) {
        save();

        if (!config.isStarted()) {
            if (portInUse(config.portLocal)) {
                System.err.println("Port in use");
                return;
            }
        }
        config.settingsComplete();
        GuiManager.closeSettings();
    }

    private void save() {
        // connection tab
        config.server = server.getText();
        config.portLocal = Math.abs(portLocal.getAsInt());

        // output tab
        config.worldOutputDir = worldOutputDir.getText();
        config.centerX = centerX.getAsInt();
        config.centerZ = centerZ.getAsInt();
        config.levelSeed = levelSeed.getAsLong();
        config.disableWorldGen = disableWorldGen.isSelected();

        // general tab
        config.extendedRenderDistance =  Math.abs((int) extendedDistance.getValue());
        config.disableMarkUnsavedChunks = !markUnsaved.isSelected();
        config.markOldChunks = markOld.isSelected();
        config.renderOtherPlayers = renderOtherPlayers.isSelected();
        config.disableInfoMessages = !enableInfoMessages.isSelected();
        config.enableCaveRenderMode = enableCaveRenderMode.isSelected();
        config.drawExtendedChunks = enableDrawExtendedChunks.isSelected();

        Config.save();
    }

    public boolean portInUse(int port) {
        if (config.isStarted()) { return false; }

        try (ServerSocket ss = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * Set the IP address given by the realms tab
     */
    public void setSelectedIp(String address) {
        this.server.setText(address);

        tabPane.getSelectionModel().selectFirst();
        this.saveButton.requestFocus();
    }
}
