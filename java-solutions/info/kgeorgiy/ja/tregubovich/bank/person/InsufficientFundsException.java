package info.kgeorgiy.ja.tregubovich.bank.person;

public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(final String message) {
        super(message);
    }
}
