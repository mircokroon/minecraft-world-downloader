package proxy;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static util.ExceptionHandling.attempt;

public class ConnectionDetails {
    private final int DEFAULT_PORT = 25565;

    private String host;
    private int portRemote;
    private int portLocal;

    public ConnectionDetails(String host, int portLocal, boolean performSrvLookup) {
        this.portRemote = DEFAULT_PORT;

        if (host.contains(":")) {
            String[] hostParts = host.split(":");
            this.host = hostParts[0];

            attempt(() -> this.portRemote = Integer.parseInt(hostParts[1]));
        } else {
            this.host = host;
        }
        this.portLocal = portLocal;

        if (performSrvLookup) {
            performSrvLookup();
        }
    }

    public String getFriendlyHost() {
        return host + (portRemote == DEFAULT_PORT ? "" : ":" + portRemote);
    }

    public String getHost() {
        return host;
    }

    public int getPortRemote() {
        return portRemote;
    }

    public int getPortLocal() {
        return portLocal;
    }

    public ServerSocket getServerSocket() throws IOException {
        return new ServerSocket(getPortLocal());
    }

    public Socket getClientSocket() throws IOException {
        return new Socket(host, portRemote);
    }

    public String getConnectionHint() {
        return "Connect to address localhost:" + portLocal + " to start downloading.";
    }


    /**
     * Checks for DNS service records of the form _minecraft._tcp.example.com. If they exist, we will replace the
     * current host and port with the ones found there.
     */
    private void performSrvLookup() {
        attempt(() -> {
            Record[] records = new Lookup("_minecraft._tcp." + host, Type.SRV).run();

            // no records were found
            if (records == null || records.length == 0) {
                return;
            }

            // if there's multiple records, we'll just take the first one
            SRVRecord srvRecord = (SRVRecord) records[0];
            portRemote = srvRecord.getPort();
            host = srvRecord.getTarget().toString(true);
        });
    }
}
