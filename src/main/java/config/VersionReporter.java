package config;

import game.protocol.Protocol;
import game.protocol.ProtocolVersionHandler;

public class VersionReporter {
    private final static int DATA_VERSION_1_12 = 1022;
    private final static int DATA_VERSION_1_13 = 1444;
    private final static int DATA_VERSION_1_14 = 1901;
    private final static int DATA_VERSION_1_15 = 2200;
    private final static int DATA_VERSION_1_16 = 2504;

    private final static int VERSION_1_12 = 317;
    private final static int VERSION_1_13 = 341;
    private final static int VERSION_1_14 = 440;
    private final static int VERSION_1_15 = 550;
    private final static int VERSION_1_16 = 701;

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
        return protocolVersion >= VERSION_1_12;
    }
    public boolean isAtLeast1_13() {
        return protocolVersion >= VERSION_1_13;
    }
    public boolean isAtLeast1_14() {
        return protocolVersion >= VERSION_1_14;
    }
    public boolean isAtLeast1_15() {
        return protocolVersion >= VERSION_1_15;
    }
    public boolean isAtLeast1_16() {
        return protocolVersion >= VERSION_1_16;
    }

    public static boolean isAtLeast1_12(int dataVersion) {
        return dataVersion >= DATA_VERSION_1_12;
    }
    public static boolean isAtLeast1_13(int dataVersion) {
        return dataVersion >= DATA_VERSION_1_13;
    }
    public static boolean isAtLeast1_14(int dataVersion) {
        return dataVersion >= DATA_VERSION_1_14;
    }
    public static boolean isAtLeast1_15(int dataVersion) {
        return dataVersion >= DATA_VERSION_1_15;
    }
    public static boolean isAtLeast1_16(int dataVersion) {
        return dataVersion >= DATA_VERSION_1_16;
    }

    public boolean is1_12() {
        return protocolVersion >= VERSION_1_12 && protocolVersion < VERSION_1_13;
    }
    public boolean is1_13() {
        return protocolVersion >= VERSION_1_13 && protocolVersion < VERSION_1_14;
    }
    public boolean is1_14() {
        return protocolVersion >= VERSION_1_14 && protocolVersion < VERSION_1_15;
    }
    public boolean is1_15() {
        return protocolVersion >= VERSION_1_15 && protocolVersion < VERSION_1_16;
    }
    public boolean is1_16() {
        return protocolVersion >= VERSION_1_16;
    }
}
