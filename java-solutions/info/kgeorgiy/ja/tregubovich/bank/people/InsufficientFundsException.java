package info.kgeorgiy.ja.tregubovich.bank.people;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
