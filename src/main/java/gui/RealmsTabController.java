package gui;

import com.google.gson.Gson;
import gui.components.NoSelectionModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import proxy.auth.AuthDetailsManager;
import proxy.auth.RealmsApiHandler;

import static util.ExceptionHandling.attempt;

/**
 * Controller for the realms tab.
 */
public class RealmsTabController {
    public ListView<RealmEntry> serverList;
    public Button loadButton;

    private boolean requested;
    private RealmsApiHandler auth;
    private GuiSettings settings;

    public TextField minecraftUsername;

    @FXML
    void initialize() {
        serverList.setFocusTraversable( false );
        serverList.setSelectionModel(new NoSelectionModel<>());
        serverList.setCellFactory(e -> new ListCell<RealmEntry>() {
            @Override
            protected void updateItem(RealmEntry realmEntry, boolean empty) {
                super.updateItem(realmEntry, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                // load tab layout from fxml file
                attempt(() -> {
                    FXMLLoader loader = new FXMLLoader(RealmsTabController.class.getResource("/ui/RealmItem.fxml"));
                    loader.setController(new RealmItemController(realmEntry, settings));

                    setGraphic(((Parent) loader.load()).lookup("#item"));
                });
            }
        });

        minecraftUsername.textProperty().addListener((a, oldText, newText) -> {
            loadButton.setDisable(newText == null || newText.length() == 0);
        });

        minecraftUsername.focusedProperty().addListener((a, wasInFocus, isInFocus) -> {
            if (wasInFocus && !isInFocus) {
                GuiManager.getConfig().username = minecraftUsername.getText().trim();
            }
        });

    }

    /**
     * Called when the tab is opened so that the realms list can be loaded.
     */
    public void opened(GuiSettings guiSettings) {
        this.settings = guiSettings;

        minecraftUsername.setText(GuiManager.getConfig().username);
    }
    
    @FXML
    private void requestList(ActionEvent actionEvent) {
        // if the tab is closed and opened again, we shouldn't restart the request
        if (requested) {
            return;
        }
        requested = true;

        // initially set list to loading text
        serverList.setItems(FXCollections.observableArrayList(new RealmEntry("Loading...")));

        auth = new RealmsApiHandler(minecraftUsername.getText());
        auth.requestRealms(str -> {
            RealmServers serversTemp = null;
            try {
                serversTemp = new Gson().fromJson(str, RealmServers.class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            final RealmServers servers = serversTemp;
            Platform.runLater(() -> attempt(() -> {
                serverList.getItems().clear();

                if (servers == null || servers.servers.length <= 0) {
                    serverList.getItems().add(new RealmEntry("No realms found for user " + auth.getAuthDetails().getUsername()));
                    this.requested = false;
                } else {
                    // each realm needs a reference to the auth object to request the server IP
                    for (RealmEntry realm : servers.servers) {
                        realm.setAuth(auth);
                        realm.reset();
                    }

                    serverList.getItems().addAll(servers.servers);
                }
            }));
        });
    }

    /**
     * Classes for JSON deserialization of API responses.
     */
    private static class RealmAddress { String address; }
    private static class RealmServers { RealmEntry[] servers; }

    /**
     * States for items in list view.
     */
    enum RealmState {
        INFO, LOADING, IP_UNKNOWN, IP_KNOWN
    }

    /**
     * Class to store realm information, used to display in GUI list.
     */
    private class RealmEntry {
        private final int id;
        private final String name, motd;

        private transient String address;
        private transient RealmState state = RealmState.IP_UNKNOWN;
        private transient int requestsLeft;
        private transient RealmItemController controller;
        private transient RealmsApiHandler auth;

        public void setAuth(RealmsApiHandler auth) {
            this.auth = auth;
        }

        /**
         * If only a server name is given, it's a placeholder (e.g. Loading text)
         */
        public RealmEntry(String name) {
            this.id = -1;
            this.name = name;
            this.motd = "";
            this.state = RealmState.INFO;
        }

        /**
         * Request the IP address of a realm. This is done by sending a Join request to the realm, which will start
         * up the server if it's currently asleep. If server is not yet active, the response will be "Retry again later"
         * which means we need to wait a little bit and try again.
         */
        private void makeIpRequest() {
            // number of requests for IP is limited so that we don't do it infinitely
            requestsLeft--;
            if (requestsLeft == 0) {
                reset();
                Platform.runLater(controller::refresh);
                return;
            }

            auth.requestRealmIp(id, res -> {
                try {
                    if (!res.contains("Retry again later") && !res.equals("")) {
                        RealmAddress address = new Gson().fromJson(res, RealmAddress.class);

                        if (address != null) {
                            setAddress(address.address);
                            return;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // if we didn't get a good response, wait a little bit and retry
                attempt(() -> Thread.sleep(4000));
                makeIpRequest();
            });
        }

        private void setAddress(String address) {
            this.address = address;
            this.state = RealmState.IP_KNOWN;
            Platform.runLater(controller::refresh);
        }

        public void reset() {
            this.state = RealmState.IP_UNKNOWN;
            requestsLeft = 20;
        }

        public void requestIp() {
            this.state = RealmState.LOADING;
            controller.refresh();

            makeIpRequest();
        }

        public void setController(RealmItemController realmItemController) {
            this.controller = realmItemController;
            this.controller.refresh();
        }

        public RealmState getState() {
            return state;
        }

        public String getAddress() {
            return address;
        }
    }

    /**
     * Class for list item in the realms list.
     */
    class RealmItemController {
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
}



