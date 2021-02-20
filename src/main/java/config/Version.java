package config;

public enum Version {
    V1_12,
    V1_13,
    V1_14,
    V1_15,
    V1_16,
    ANY;

    boolean isVersion(VersionReporter versionReporter) {
        switch (this) {
            case V1_12: return versionReporter.isAtLeast1_12();
            case V1_13: return versionReporter.isAtLeast1_13();
            case V1_14: return versionReporter.isAtLeast1_14();
            case V1_15: return versionReporter.isAtLeast1_15();
            case V1_16: return versionReporter.isAtLeast1_16();
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
            case ANY: return true;
            default: return false;
        }
    }
}
