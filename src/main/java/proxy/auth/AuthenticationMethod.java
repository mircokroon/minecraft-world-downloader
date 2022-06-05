package proxy.auth;

public enum AuthenticationMethod {
    AUTOMATIC("Automatic", "No session found. Ensure the game is running." +
        " If you are not using the default launcher or client," +
        " you may have to use an alternative authentication method."),

    MICROSOFT("Microsoft login","No Microsoft login found, or session has " +
        "expired. Press the button below to login."),

    MANUAL("Manually enter details", "Given access token is not valid.");

    private final String label;
    private final String errorMessage;

    AuthenticationMethod(String label, String errorMessage) {
        this.label = label;
        this.errorMessage = errorMessage;
    }

    public String getLabel() {
        return label;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
