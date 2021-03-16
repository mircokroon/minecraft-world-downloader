package gui.realms;

import com.google.gson.Gson;
import gui.GuiSettings;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import proxy.auth.ClientAuthenticator;

import java.io.IOException;
import java.util.Arrays;

public class RealmsTabController {
    public ListView<RealmEntry> serverList;

    public RealmsTabController() { }

    private boolean requested;

    private ClientAuthenticator auth;

    private GuiSettings settings;

    @FXML
    void initialize() {
        serverList.setCellFactory(e -> new ListCell<RealmEntry>() {
            Parent node;

            @Override
            protected void updateItem(RealmEntry realmEntry, boolean empty) {
                super.updateItem(realmEntry, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                if (node == null) {
                    try {
                        FXMLLoader loader = new FXMLLoader(RealmsTabController.class.getResource("/ui/RealmItem.fxml"));
                        loader.setController(new RealmItemController(realmEntry, settings));

                        node = loader.load();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        return;
                    }
                }
                setGraphic(node);
            }
        });
    }

    public void opened(GuiSettings guiSettings) {
        if (requested) {
            return;
        }
        requested = true;
        this.settings = guiSettings;

        serverList.getItems().add(new RealmEntry("Loading..."));

        auth = new ClientAuthenticator();
        auth.requestRealms(str -> {
            RealmServers servers = new Gson().fromJson(str, RealmServers.class);

            Platform.runLater(() -> {
                serverList.getItems().clear();

                if (servers.servers.length > 0) {
                    Arrays.stream(servers.servers).forEach(realm -> {
                        realm.setAuth(auth);
                        realm.reset();
                    });
                    serverList.getItems().addAll(servers.servers);
                } else {
                    serverList.getItems().add(new RealmEntry("No realms found"));
                }
            });
        });
    }

    private static class RealmServers { RealmEntry[] servers; }
}



