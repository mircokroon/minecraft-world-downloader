package gui;


import config.Config;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

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
    Config config;

    public GuiSettings() {
        this.config = GuiManager.getConfig();
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
