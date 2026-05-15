package info.kgeorgiy.ja.tregubovich.bank.account;

public class NegativeBalanceException extends RuntimeException {
    public NegativeBalanceException(final String message) {
        super(message);
    }
}
