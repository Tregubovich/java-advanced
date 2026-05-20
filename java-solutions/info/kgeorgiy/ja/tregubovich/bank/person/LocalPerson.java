package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.LocalAccount;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LocalPerson extends Person implements Serializable {
    private final Map<String, LocalAccount> accounts = new HashMap<>();

    public LocalPerson(final String name, final String surname, final int passportId) {
        this(name, surname, passportId, Map.of());
    }

    public LocalPerson(final String name, final String surname, final int passportId, final Map<String, Account> accounts) {
        super(name, surname, passportId);
        accounts.values().forEach(account -> this.accounts.put(account.getId().split(":")[1], new LocalAccount(account)));
    }

    public LocalPerson(final Person person) {
        this(person.getName(), person.getSurname(), person.getPassportId(), person.getAccounts());
    }

    @Override
    public Map<String, Account> getAccounts() {
        return new HashMap<>(accounts);
    }

    @Override
    protected Account createAccount(final String id) {
        final Account account = accounts.putIfAbsent(id, new LocalAccount(getFullAccountId(id)));
        if (account == null) {
            System.out.println("Creating account " + getFullAccountId(id));
        }
        return accounts.get(id);
    }
}
