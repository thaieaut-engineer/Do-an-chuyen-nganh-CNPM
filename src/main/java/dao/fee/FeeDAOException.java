package dao.fee;
public class FeeDAOException extends RuntimeException {
    public FeeDAOException(String message){super(message);}
    public FeeDAOException(String message,Throwable cause){super(message,cause);}
}
