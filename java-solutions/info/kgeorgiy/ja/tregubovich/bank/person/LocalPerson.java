package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.LocalAccount;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class LocalPerson extends AbstractPerson implements Serializable {
    private final Map<String, Account> accounts = new HashMap<>();

    public LocalPerson(final String name, final String surname, final int passportID) {
        this(name, surname, passportID, Map.of());
    }

    public LocalPerson(final String name, final String surname, final int passportID, final Map<String, Account> accounts) {
        super(name, surname, passportID);
        accounts.forEach((key, value) -> {
            try {
                this.accounts.put(key, new LocalAccount(value));
            } catch (final RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public LocalPerson(final Person person) {
        this(person.getName(), person.getSurname(), person.getPassportID(), person.getAccounts());
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    protected Account createAccount(final String id) {
        if (accounts.containsKey(id)) {
            return accounts.get(id);
        }
        System.out.println("Creating account " + getFullAccountID(id));
        final Account account = new LocalAccount(getFullAccountID(id));
        accounts.put(id, account);
        return account;
    }
}
