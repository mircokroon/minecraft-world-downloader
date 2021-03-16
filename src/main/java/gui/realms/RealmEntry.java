package gui.realms;

import com.google.gson.Gson;
import javafx.application.Platform;
import proxy.auth.ClientAuthenticator;

import static util.ExceptionHandling.attempt;

public class RealmEntry {
    int id;
    String name;
    String motd;

    private transient String address;
    private transient RealmState state = RealmState.IP_UNKNOWN;
    private transient int requestsLeft;
    private transient RealmItemController controller;
    private transient ClientAuthenticator auth;

    public RealmEntry() { }

    public void setAuth(ClientAuthenticator auth) {
        this.auth = auth;
    }

    public RealmEntry(String name) {
        this.id = -1;
        this.name = name;
        this.motd = "";
        this.state = RealmState.PLACEHOLDER;
    }

    private void makeIpRequest() {
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

    private static class RealmAddress { String address; }
}
