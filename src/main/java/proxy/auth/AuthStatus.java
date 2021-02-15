package proxy.auth;

/**
 * Class to keep track of user's details authentication status. When the details are present but old they will be
 * marked as probably expired.
 */
public class AuthStatus {
    boolean isValid;
    boolean isProbablyExpired;
    String message;

    public AuthStatus(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }

    public boolean isExpired() {
        return isProbablyExpired;
    }

    public boolean isValid() {
        return isValid;
    }
}
