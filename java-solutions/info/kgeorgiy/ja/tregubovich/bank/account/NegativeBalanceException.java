package info.kgeorgiy.ja.tregubovich.bank.account;

public class NegativeBalanceException extends Exception {
    public NegativeBalanceException(final String message) {
        super(message);
    }
}
