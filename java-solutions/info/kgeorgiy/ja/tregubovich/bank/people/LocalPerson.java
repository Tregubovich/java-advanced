package info.kgeorgiy.ja.tregubovich.bank.people;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.LocalAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    private final Map<String, Account> accounts = new HashMap<>();

    public LocalPerson(final String name, final String surname, final int passportId) {
        this(name, surname, passportId, null);
    }

    public LocalPerson(final String name, final String surname, final int passportId, final Map<String, Account> accounts) {
        super(name, surname, passportId);
        this.accounts.putAll(accounts);
    }

    public LocalPerson(final Person person) throws RemoteException {
        this(person.getName(), person.getSurname(), person.getPassportId(), person.getAccounts());
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    protected Account createAccount(final String id) throws RemoteException {
        final Account account = accounts.putIfAbsent(id, new LocalAccount(getFullAccountId(id)));
        if (account == null) {
            System.out.println("Creating account " + getFullAccountId(id));
        }
        return accounts.get(id);
    }
}
