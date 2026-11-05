package bus.auth;

public final class AuthenticationException extends Exception {
    public enum Reason {
        INVALID_INPUT,
        INVALID_CREDENTIALS,
        CURRENT_PASSWORD_INCORRECT,
        PASSWORD_CONFIRMATION_MISMATCH,
        PASSWORD_POLICY_VIOLATION,
        SESSION_EXPIRED,
        ACCOUNT_LOCKED,
        ACCOUNT_PENDING,
        ACCOUNT_REVOKED,
        ACCOUNT_DISABLED,
        EMPLOYEE_INACTIVE,
        PASSWORD_NOT_CONFIGURED,
        NO_ACTIVE_ROLE,
        NO_PERMISSION,
        DATABASE_ERROR
    }

    private final Reason reason;

    public AuthenticationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AuthenticationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
