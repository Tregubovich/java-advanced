package info.kgeorgiy.ja.tregubovich.bank.bank;

public class PersonAlreadyExistException extends RuntimeException {
    public PersonAlreadyExistException(final String message) {
        super(message);
    }
}
