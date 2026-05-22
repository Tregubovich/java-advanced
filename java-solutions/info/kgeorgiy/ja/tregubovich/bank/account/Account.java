package info.kgeorgiy.ja.tregubovich.bank.account;

public interface Account {
    /** Returns account identifier. */
    String getID();

    /** Returns amount of money in the account. */
    int getBalance();

    /** Sets amount of money in the account. */
    void setBalance(int amount) throws NegativeBalanceException;
}
