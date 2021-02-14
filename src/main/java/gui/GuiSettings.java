package gui;


import config.Config;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.util.Arrays;
import java.util.List;

public class GuiSettings {
    public TextField server;
    public TextField portRemote;
    public TextField portLocal;

    public TextField worldOutputDir;
    public TextField centerX;
    public TextField centerZ;
    public TextField levelSeed;

    public TextField extendedDistance;
    public CheckBox measureRenderDistance;
    public CheckBox markUnsaved;
    public TextField overviewZoom;

    public Button saveButton;
    public Tab errTab;
    public TabPane tabPane;
    public TextArea errOutput;
    public TextField minecraftDir;
    Config config;

    public GuiSettings() {
        this.config = GuiManager.getConfig();
        GuiManager.registerSettingController(this);
    }

    @FXML
    void initialize() {
        if (config.isStarted()) {
            saveButton.setText("Save");
        }

        // connection tab
        server.setText(config.server);
        portRemote.setText("" + config.portRemote);
        portLocal.setText("" + config.portLocal);
        minecraftDir.setText(Config.getMinecraftPath());

        // output tab
        worldOutputDir.setText(config.worldOutputDir);
        centerX.setText("" + config.centerX);
        centerZ.setText("" + config.centerZ);
        levelSeed.setText("" + config.levelSeed);

        // general tab
        extendedDistance.setText("" + config.extendedRenderDistance);
        measureRenderDistance.setSelected(config.measureRenderDistance);
        markUnsaved.setSelected(!config.disableMarkUnsavedChunks);
        overviewZoom.setText("" + config.zoomLevel);

        numeric(Arrays.asList(portRemote, portLocal, centerX, centerZ, levelSeed, extendedDistance, overviewZoom));
        disableWhenRunning(Arrays.asList(server, portRemote, portLocal, centerX, centerZ, worldOutputDir));

        handleErrorTab();

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
        } else {
            tabPane.getSelectionModel().select(errTab);
        }
        errOutput.setWrapText(true);

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

    private void disableWhenRunning(List<TextField> numericFields) {
        if (!config.isStarted()) {
            return;
        }
        numericFields.forEach(field -> field.setDisable(true));
    }

    private void numeric(List<TextField> numericFields) {
        numericFields.forEach(field -> field.textProperty().addListener((observable, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(oldVal);
            }
        }));
    }

    public void saveSettings(ActionEvent actionEvent) {
        // connection tab
        config.server = server.getText();
        config.portRemote = Integer.parseInt(portRemote.getText());
        config.portLocal = Integer.parseInt(portLocal.getText());
        config.minecraftDir = minecraftDir.getText();

        // output tab
        config.worldOutputDir = worldOutputDir.getText();
        config.centerX = Integer.parseInt(centerX.getText());
        config.centerZ = Integer.parseInt(centerZ.getText());
        config.levelSeed = Integer.parseInt(levelSeed.getText());

        // general tab
        config.extendedRenderDistance = Integer.parseInt(extendedDistance.getText());
        config.measureRenderDistance = measureRenderDistance.isSelected();
        config.disableMarkUnsavedChunks = !markUnsaved.isSelected();
        config.zoomLevel = Integer.parseInt(overviewZoom.getText());


        config.settingsComplete();
    }
}
