package config;

public enum Version {
    // version numbers correspond to the earliest full release
    V1_12(317, 1132),
    V1_13(341, 1444),
    V1_14(440, 1901),
    V1_15(550, 2200),
    V1_16(701, 2578),
    V1_17(755, 2724),
    V1_18(757, 2860),
    ANY(0, 0);

    public final int dataVersion;
    public final int protocolVersion;

    Version(int protocolVersion, int dataVersion) {
        this.protocolVersion = protocolVersion;
        this.dataVersion = dataVersion;
    }

    boolean isVersion(VersionReporter versionReporter) {
        return switch (this) {
            case V1_12 -> versionReporter.isAtLeast1_12();
            case V1_13 -> versionReporter.isAtLeast1_13();
            case V1_14 -> versionReporter.isAtLeast1_14();
            case V1_15 -> versionReporter.isAtLeast1_15();
            case V1_16 -> versionReporter.isAtLeast1_16();
            case V1_17 -> versionReporter.isAtLeast1_17();
            case V1_18 -> versionReporter.isAtLeast1_18();
            case ANY -> true;
            default -> false;
        };
    }

    public boolean isDataVersion(int dataVersion) {
        switch (this) {
            case V1_12: return VersionReporter.isAtLeast1_12(dataVersion);
            case V1_13: return VersionReporter.isAtLeast1_13(dataVersion);
            case V1_14: return VersionReporter.isAtLeast1_14(dataVersion);
            case V1_15: return VersionReporter.isAtLeast1_15(dataVersion);
            case V1_16: return VersionReporter.isAtLeast1_16(dataVersion);
            case V1_17: return VersionReporter.isAtLeast1_17(dataVersion);
            case V1_18: return VersionReporter.isAtLeast1_18(dataVersion);
            case ANY: return true;
            default: return false;
        }
    }
}
