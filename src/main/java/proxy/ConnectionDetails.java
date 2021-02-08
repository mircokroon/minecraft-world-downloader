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

    public ConnectionDetails(String host, int portRemote, int portLocal, boolean performSrvLookup) {
        this.host = host;
        this.portRemote = portRemote;
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
