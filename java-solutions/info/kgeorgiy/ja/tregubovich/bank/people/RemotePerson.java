package info.kgeorgiy.ja.tregubovich.bank.people;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.RemoteAccount;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemotePerson extends AbstractPerson {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    public RemotePerson(final String name, final String surname, final int passportNumber, final int port) {
        super(name, surname, passportNumber);
        this.port = port;
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    protected Account createAccount(final String id) throws RemoteException {
        final Account account = new RemoteAccount(getFullAccountId(id));
        if (accounts.putIfAbsent(id, account) == null) {
            System.out.println("Creating account " + getFullAccountId(id));
            UnicastRemoteObject.exportObject(account, port);
        }
        return accounts.get(id);
    }
}
