package info.kgeorgiy.ja.tregubovich.bank.bank;

public class PersonAlreadyExistException extends Exception {
    public PersonAlreadyExistException(final String message) {
        super(message);
    }
}
