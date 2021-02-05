package util;

import java.util.Arrays;

public class ToString {
    public static String array(Object[] arr) {
        if (arr.length < 4) {
            return Arrays.toString(arr);
        }
        return "[" + arr[0] + ", ... (x" + (arr.length - 2) + "), " + arr[arr.length - 1] + "]";
    }

    public static String array(int[] arr) {
        if (arr.length < 4) {
            return Arrays.toString(arr);
        }
        return "[" + arr[0] + ", ... (x" + (arr.length - 2) + "), " + arr[arr.length - 1] + "]";
    }

    public static String array(long[] arr) {
        if (arr.length < 4) {
            return Arrays.toString(arr);
        }
        return "[" + arr[0] + ", ... (x" + (arr.length - 2) + "), " + arr[arr.length - 1] + "]";
    }

    public static String array(byte[] arr) {
        if (arr.length < 4) {
            return Arrays.toString(arr);
        }
        return "[" + arr[0] + ", ... (x" + (arr.length - 2) + "), " + arr[arr.length - 1] + "]";
    }
}
