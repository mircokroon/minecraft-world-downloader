package config;

import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;

public class VersionReporter {
    private final Protocol protocol;

    public VersionReporter(int protocolVersion) {
        this.protocol = ProtocolVersionHandler.getInstance().getProtocolByProtocolVersion(protocolVersion);
    }

    public int getDataVersion() {
        return protocol.getDataVersion();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public static <T> T select(int dataVersion, Class<T> type, Option... opts) {
        for (Option opt : opts) {
            if (dataVersion >= opt.v.dataVersion) {
                return type.cast(opt.obj.get());
            }
        }
        return null;
    }

    public <T> T select(Class<T> type, Option... opts) {
        for (Option opt : opts) {
            if (isAtLeast(opt.v)) {
                return type.cast(opt.obj.get());
            }
        }
        return null;
    }

    public boolean isAtLeast(Version v) {
        return getDataVersion() >= v.dataVersion;
    }
}
