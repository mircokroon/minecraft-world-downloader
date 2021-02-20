package game.data.chunk;

public class IncompleteChunkException extends RuntimeException {
    public IncompleteChunkException() {
    }

    public IncompleteChunkException(String message) {
        super(message);
    }

    public IncompleteChunkException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteChunkException(Throwable cause) {
        super(cause);
    }

    public IncompleteChunkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
