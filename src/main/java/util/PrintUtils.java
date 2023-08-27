package util;

import config.Config;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

public class PrintUtils {
    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');

        formatter = new DecimalFormat("###,###", symbols);
    }

    public static void devPrint(String out) {
        if (Config.isInDevMode()) {
            System.out.println(out);
        }
    }

    public static void devPrintFormat(String out, Object... args) {
        if (Config.isInDevMode()) {
            System.out.format(out, args);
        }
    }

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

    public static String humanReadable(int number) {
        return formatter.format(number);
    }

}
