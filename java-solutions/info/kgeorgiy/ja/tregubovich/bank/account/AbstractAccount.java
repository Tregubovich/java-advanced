package info.kgeorgiy.ja.tregubovich.bank.account;

public abstract class AbstractAccount implements Account {
    private final String id;
    private int amount;

    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) throws NegativeBalanceException {
        System.out.println("Setting amount of money for account " + id);
        if  (amount < 0) {
            throw new NegativeBalanceException("Negative amount");
        }
        this.amount = amount;
    }
}
