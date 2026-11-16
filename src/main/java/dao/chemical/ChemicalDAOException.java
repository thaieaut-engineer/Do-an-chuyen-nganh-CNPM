package dao.chemical;

public class ChemicalDAOException extends RuntimeException {
    public ChemicalDAOException(String message) { super(message); }
    public ChemicalDAOException(String message, Throwable cause) { super(message, cause); }
}
