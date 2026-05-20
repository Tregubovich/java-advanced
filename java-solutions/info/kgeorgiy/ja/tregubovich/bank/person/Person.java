package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.NegativeBalanceException;

import java.rmi.RemoteException;
import java.util.Map;

public abstract class Person {
    private final String name;
    private final String surname;
    private final int passportId;

    Person(final String name, final String surname, final int passportId) {
        this.name = name;
        this.surname = surname;
        this.passportId = passportId;
    }

    /**
     * Returns person's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns person's surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Returns person's passport id
     */
    public int getPassportId() {
        return passportId;
    }

    /**
     * Returns full account id
     */
    protected String getFullAccountId(final String id) {
        return getPassportId() + ":" + id;
    }

    /**
     * Returns person's accounts
     */
    public abstract Map<String, Account> getAccounts();

    /**
     * Adds or removes money from account with specified identifier.
     * If account doesn't exist creates new with empty balance.
     *
     * @param amount amount of money
     * @param id     account id
     * @throws InsufficientFundsException if there are not enough funds in the account
     */
    public synchronized void deposit(final String id, final int amount) throws InsufficientFundsException, RemoteException {
        System.out.println("Deposit " + getFullAccountId(id) + " " + (amount > 0 ? "+" : "") + amount);
        final Account account = createAccount(id);
        final int funds = getBalance(id);
        try {
            account.setBalance(funds + amount);
        } catch (final NegativeBalanceException _) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        System.out.println("New balance of " + getFullAccountId(id) + ": " + (funds + amount));
    }

    /**
     * Returns balance of the account with specified identifier.
     * If account doesn't exist creates new with empty balance.
     *
     * @return amount of funds on account
     */
    public int getBalance(final String id) throws RemoteException {
        final Account account = createAccount(id);
        final int funds = account.getBalance();
        System.out.println("Balance of " + getFullAccountId(id) + ": " + funds);
        return funds;
    }

    /**
     * Creates a new account with specified identifier if it does not already exist.
     *
     * @param id account id
     * @return created or existing account.
     */
    protected abstract Account createAccount(final String id) throws RemoteException;
}
