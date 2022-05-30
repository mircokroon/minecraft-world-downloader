package packets;

public class UUID {
    long lower;
    long upper;

    public UUID(String uuid) {
        String stripped = uuid.replace("-", "");
        this.lower = Long.parseUnsignedLong(stripped.substring(0, 16), 16);
        this.upper = Long.parseUnsignedLong(stripped.substring(16, 32), 16);
    }

    public UUID(long lower, long upper) {
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        UUID uuid = (UUID) o;

        if (lower != uuid.lower) { return false; }
        return upper == uuid.upper;
    }

    @Override
    public int hashCode() {
        int result = (int) (lower ^ (lower >>> 32));
        result = 31 * result + (int) (upper ^ (upper >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%016X", lower) + String.format("%016X", upper);
    }

    public long getUpper() {
        return upper;
    }

    public long getLower() {
        return lower;
    }

    public int[] asIntArray() {
        long long1 = upper;
        long long2 = lower;
        return new int[]{(int) (long1 >> 32), (int) long1, (int) (long2 >> 32), (int) long2};
    }
}
