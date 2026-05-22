package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.NegativeBalanceException;

import java.rmi.RemoteException;
import java.util.Map;

public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final int passportID;

    AbstractPerson(final String name, final String surname, final int passportID) {
        this.name = name;
        this.surname = surname;
        this.passportID = passportID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public int getPassportID() {
        return passportID;
    }

    /**
     * Returns full account id in format {passportID:ID}
     */
    protected String getFullAccountID(final String id) {
        return getPassportID() + ":" + id;
    }

    @Override
    public abstract Map<String, Account> getAccounts();

    @Override
    public synchronized void deposit(final String id, final int amount) throws InsufficientFundsException {
        System.out.println("Deposit " + getFullAccountID(id) + " " + (amount > 0 ? "+" : "") + amount);
        final Account account;
        try {
            account = createAccount(id);
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        }
        final int funds = getBalance(id);
        try {
            account.setBalance(funds + amount);
        } catch (final NegativeBalanceException _) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        System.out.println("New balance of " + getFullAccountID(id) + ": " + (funds + amount));
    }

    @Override
    public int getBalance(final String id) {
        final Account account;
        try {
            account = createAccount(id);
        } catch (final RemoteException e) {
            throw new RuntimeException(e);
        }
        final int funds = account.getBalance();
        System.out.println("Balance of " + getFullAccountID(id) + ": " + funds);
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
