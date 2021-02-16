package util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handle working directory since there is no easy to way to set it in Java.
 */
public class PathUtils {
    private static String workingDirectory = "abc";

    public static Path toPath(String... parts) {
        // if the path is absolute, don't add the working dir
        Path onlyEnd = Paths.get("", parts);
        if (onlyEnd.isAbsolute()) {
            return onlyEnd;
        }

        return Paths.get(workingDirectory, parts).toAbsolutePath();
    }

    public static void setWorkingDirectory(String dir) {
        workingDirectory = dir;
    }

    public static void setWorkingDirectory(Path p) {
        workingDirectory = p.toAbsolutePath().toString();
    }
}
