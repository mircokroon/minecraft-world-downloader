package config;

public enum Version {
    // version numbers correspond to the earlier full release
    V1_12(317, 1139),
    V1_13(341, 1519),
    V1_14(440, 1952),
    V1_15(550, 2225),
    V1_16(701, 2578),
    V1_17(755, 2724),
    ANY(0, 0);

    public final int dataVersion;
    public final int protocolVersion;

    Version(int protocolVersion, int dataVersion) {
        this.protocolVersion = protocolVersion;
        this.dataVersion = dataVersion;
    }

    boolean isVersion(VersionReporter versionReporter) {
        switch (this) {
            case V1_12: return versionReporter.isAtLeast1_12();
            case V1_13: return versionReporter.isAtLeast1_13();
            case V1_14: return versionReporter.isAtLeast1_14();
            case V1_15: return versionReporter.isAtLeast1_15();
            case V1_16: return versionReporter.isAtLeast1_16();
            case V1_17: return versionReporter.isAtLeast1_17();
            case ANY: return true;
            default: return false;
        }
    }

    public boolean isDataVersion(int dataVersion) {
        switch (this) {
            case V1_12: return VersionReporter.isAtLeast1_12(dataVersion);
            case V1_13: return VersionReporter.isAtLeast1_13(dataVersion);
            case V1_14: return VersionReporter.isAtLeast1_14(dataVersion);
            case V1_15: return VersionReporter.isAtLeast1_15(dataVersion);
            case V1_16: return VersionReporter.isAtLeast1_16(dataVersion);
            case V1_17: return VersionReporter.isAtLeast1_17(dataVersion);
            case ANY: return true;
            default: return false;
        }
    }
}
