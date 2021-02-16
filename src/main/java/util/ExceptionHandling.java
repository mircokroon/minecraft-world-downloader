package util;

import proxy.IExceptionConsumer;
import proxy.IExceptionHandler;

public class ExceptionHandling {
    /**
     * Method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r, IExceptionConsumer failure) {
        try {
            r.run();
        } catch (Exception ex) {
            failure.consume(ex);
        }
    }

    /**
     * Method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r) {
        attempt(r, Throwable::printStackTrace);
    }


    public static void attemptQuiet(IExceptionHandler r) {
        attempt(r, (e) -> {});
    }
}
