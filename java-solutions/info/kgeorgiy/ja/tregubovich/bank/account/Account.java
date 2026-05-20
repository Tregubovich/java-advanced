package info.kgeorgiy.ja.tregubovich.bank.account;

public abstract class Account {
    private final String id;
    private int balance;

    protected Account(final String id) {
        this(id, 0);
    }

    protected Account(final String id, final int balance) {
        this.id = id;
        this.balance = balance;
    }

    /** Returns account identifier. */
    public String getId() {
        return id;
    }

    /** Returns amount of money in the account. */
    public synchronized int getBalance() {
        System.out.println("Getting amount of money for account " + id);
        return balance;
    }

    /** Sets amount of money in the account. */
    public synchronized void setBalance(final int balance) throws NegativeBalanceException {
        System.out.println("Setting amount of money for account " + id);
        if  (balance < 0) {
            throw new NegativeBalanceException("Negative amount");
        }
        this.balance = balance;
    }
}
