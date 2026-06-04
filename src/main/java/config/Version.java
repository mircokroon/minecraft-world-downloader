package config;

public enum Version {
    // version numbers correspond to the earliest full release. For 1.8-1.11 the protocol/data values are
    // synthetic lower-bound anchors so that bestMatch maps each version family to the right handler
    // (1.8 has no real data version; 1.9-1.11 use their release data versions).
    V1_8(47, 100),
    V1_9(107, 169),
    V1_10(210, 510),
    V1_11(315, 819),
    V1_12(317, 1132),
    V1_13(341, 1444),
    V1_14(440, 1901),
    V1_15(550, 2200),
    V1_16(701, 2578),
    V1_17(755, 2724),
    V1_18(757, 2860),
    V1_19(759, 3105),
    V1_19_3(761, 3218),
    V1_20(763, 3463),
    V1_20_2(764, 3578),
    V1_20_4(765, 3698),
    V1_20_6(766, 3839),
    V1_21(767, 3953),
    // 1.21.5 changed the chunk-data heightmaps from an NBT compound to a length-prefixed array; this
    // anchor marks that boundary so 1.21.5-1.21.11 use the array-aware chunk handling.
    V1_21_5(770, 4325),
    // First year-based release (26.1 "Tiny Takeover"). Protocol/data versions per the Minecraft Wiki;
    // packet IDs taken from protocol 774 (1.21.11), the closest version with machine-readable protocol data.
    V26_1(775, 4786),
    ANY(0, 0);

    public final int dataVersion;
    public final int protocolVersion;

    Version(int protocolVersion, int dataVersion) {
        this.protocolVersion = protocolVersion;
        this.dataVersion = dataVersion;
    }
}
