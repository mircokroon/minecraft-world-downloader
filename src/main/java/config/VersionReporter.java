package config;

import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;

public class VersionReporter {
    private final int protocolVersion;
    public VersionReporter(int version) {
        this.protocolVersion = version;
    }

    public Protocol getProtocol() {
        return ProtocolVersionHandler.getInstance().getProtocolByProtocolVersion(protocolVersion);
    }

    public static <T> T select(int dataVersion, Class<T> type, Option... opts) {
        for (Option opt : opts) {
            if (is(dataVersion, opt.v)) {
                return type.cast(opt.obj.get());
            }
        }
        return null;
    }

    public <T> T select(Class<T> type, Option... opts) {
        for (Option opt : opts) {
            if (is(opt.v)) {
                return type.cast(opt.obj.get());
            }
        }
        return null;
    }

    public static boolean is(int dataVersion, Version v) {
        return v.isDataVersion(dataVersion);
    }

    public boolean is(Version v) {
        return v.isVersion(this);
    }

    public boolean isAtLeast1_12() {
        return protocolVersion >= Version.V1_12.protocolVersion;
    }
    public boolean isAtLeast1_13() {
        return protocolVersion >= Version.V1_13.protocolVersion;
    }
    public boolean isAtLeast1_14() {
        return protocolVersion >= Version.V1_14.protocolVersion;
    }
    public boolean isAtLeast1_15() {
        return protocolVersion >= Version.V1_15.protocolVersion;
    }
    public boolean isAtLeast1_16() {
        return protocolVersion >= Version.V1_16.protocolVersion;
    }
    public boolean isAtLeast1_17() {
        return protocolVersion >= Version.V1_17.protocolVersion;
    }

    public static boolean isAtLeast1_12(int dataVersion) {
        return dataVersion >= Version.V1_12.dataVersion;
    }
    public static boolean isAtLeast1_13(int dataVersion) {
        return dataVersion >= Version.V1_13.dataVersion;
    }
    public static boolean isAtLeast1_14(int dataVersion) {
        return dataVersion >= Version.V1_14.dataVersion;
    }
    public static boolean isAtLeast1_15(int dataVersion) {
        return dataVersion >= Version.V1_15.dataVersion;
    }
    public static boolean isAtLeast1_16(int dataVersion) {
        return dataVersion >= Version.V1_16.dataVersion;
    }
    public static boolean isAtLeast1_17(int dataVersion) {
        return dataVersion >= Version.V1_17.dataVersion;
    }

    public boolean is1_12() {
        return protocolVersion >= Version.V1_12.protocolVersion && protocolVersion < Version.V1_13.protocolVersion;
    }
    public boolean is1_13() {
        return protocolVersion >= Version.V1_13.protocolVersion && protocolVersion < Version.V1_14.protocolVersion;
    }
    public boolean is1_14() {
        return protocolVersion >= Version.V1_14.protocolVersion && protocolVersion < Version.V1_15.protocolVersion;
    }
    public boolean is1_15() {
        return protocolVersion >= Version.V1_15.protocolVersion && protocolVersion < Version.V1_16.protocolVersion;
    }
    public boolean is1_16() {
        return protocolVersion >= Version.V1_16.protocolVersion && protocolVersion < Version.V1_17.protocolVersion;
    }
    public boolean is1_17() {
        return protocolVersion >= Version.V1_17.protocolVersion;
    }
}
