package proxy.auth;

public class AuthStatus {
    static final AuthStatus VALID = new AuthStatus(true, "");

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
