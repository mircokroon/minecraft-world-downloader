package gui.realms;

import gui.GuiSettings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import static util.ExceptionHandling.attempt;

public class RealmItemController {
    @FXML
    Label server, motd;

    @FXML
    Button useButton, requestButton;

    @FXML
    ImageView loading;

    RealmEntry realmEntry;

    GuiSettings settings;

    public RealmItemController(RealmEntry realmEntry, GuiSettings settings) {
        this.realmEntry = realmEntry;
        this.settings = settings;
    }

    @FXML
    void initialize() {
        realmEntry.setController(this);

        requestButton.setOnAction(e -> realmEntry.requestIp());
        useButton.setOnAction(e -> attempt(() -> settings.setSelectedIp(realmEntry.getAddress())));
    }

    void refresh() {
        server.setText(realmEntry.name);
        motd.setText(realmEntry.motd);

        loading.setVisible(realmEntry.getState() == RealmState.LOADING);
        requestButton.setVisible(realmEntry.getState() == RealmState.IP_UNKNOWN);
        useButton.setVisible(realmEntry.getState() == RealmState.IP_KNOWN);
    }
}