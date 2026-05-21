package info.kgeorgiy.ja.tregubovich.bank.account;

public abstract class AbstractAccount implements Account{
    private final String id;
    private int balance;

    protected AbstractAccount(final String id) {
        this(id, 0);
    }

    protected AbstractAccount(final String id, final int balance) {
        this.id = id;
        this.balance = balance;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public synchronized int getBalance() {
        System.out.println("Getting amount of money for account " + id);
        return balance;
    }

    @Override
    public synchronized void setBalance(final int balance) throws NegativeBalanceException {
        System.out.println("Setting amount of money for account " + id);
        if  (balance < 0) {
            throw new NegativeBalanceException("Negative amount");
        }
        this.balance = balance;
    }
}
