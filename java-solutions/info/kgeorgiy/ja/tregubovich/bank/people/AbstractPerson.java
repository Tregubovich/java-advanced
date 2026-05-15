package info.kgeorgiy.ja.tregubovich.bank.people;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.NegativeBalanceException;

import java.rmi.RemoteException;

public abstract class AbstractPerson implements Person {
    private final String name;
    private final String surname;
    private final int passportId;

    AbstractPerson(final String name, final String surname, final int passportId) {
        this.name = name;
        this.surname = surname;
        this.passportId = passportId;
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }

    @Override
    public int getPassportId() throws RemoteException {
        return passportId;
    }

    protected String getFullAccountId(final String id) throws RemoteException {
        return getPassportId() + ":" + id;
    }

    @Override
    public void deposit(final String id, final int amount) throws InsufficientFundsException, RemoteException {
        System.out.println("Deposit " + getFullAccountId(id) + " " + (amount > 0 ? "+" : "") + amount);
        final Account account = createAccount(id);
        final int funds = getBalance(id);
        try {
            account.setAmount(funds + amount);
        } catch (final NegativeBalanceException _) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        System.out.println("New balance of " + getFullAccountId(id) + ": " + (funds + amount));
    }

    @Override
    public int getBalance(final String id) throws RemoteException {
        final Account account = createAccount(id);
        final int funds = account.getAmount();
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
