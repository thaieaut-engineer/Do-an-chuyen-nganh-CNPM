package bus.account;
public final class AccountException extends RuntimeException {
    public AccountException(String message){super(message);}
    public AccountException(String message, Throwable cause){super(message,cause);}
}
