package util;

import proxy.IExceptionConsumer;
import proxy.IExceptionHandler;

public class ExceptionHandling {
    /**
     * Simple method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r, IExceptionConsumer failure) {
        try {
            r.run();
        } catch (Exception ex) {
            failure.consume(ex);
        }
    }

    /**
     * Simple method to make exception handling cleaner.
     */
    public static void attempt(IExceptionHandler r) {
        attempt(r, Throwable::printStackTrace);
    }
}
