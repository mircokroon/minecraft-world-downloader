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
    V1_19(759, 3105),
    V1_19_3(761, 3218),
    V1_20(763, 3463),
    V1_20_2(764, 3578),
    V1_20_4(765, 3698),
    V1_20_6(766, 3839),
    V1_21(767, 3953),
    V1_21_2(768, 4080), //1.21.3 has compatible protocol
    ANY(0, 0);

    public final int dataVersion;
    public final int protocolVersion;

    Version(int protocolVersion, int dataVersion) {
        this.protocolVersion = protocolVersion;
        this.dataVersion = dataVersion;
    }
}
