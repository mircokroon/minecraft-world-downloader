package old;

import java.util.Arrays;

public class ByteArrayReader {
    private byte[] arr;
    private int pos = 0;

    public ByteArrayReader(byte[] arr) {
        this.arr = arr;
    }

    public byte[] getRemaining() {
        return Arrays.copyOfRange(arr, pos, arr.length);
    }

    public byte[] read(int amount) {
        byte[] res = new byte[amount];
        System.arraycopy(arr, pos, res, 0, amount);
        pos += amount;
        return res;
    }

    public void advance(int amount) {
        pos += amount;
    }

    public byte read() {
        return arr[pos++];
    }

    public int totalSize() {
        return arr.length;
    }

    public int remainingSize() {
        return arr.length - pos;
    }
}
