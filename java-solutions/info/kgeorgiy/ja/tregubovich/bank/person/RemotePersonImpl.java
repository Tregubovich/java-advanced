package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.RemoteAccountImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePersonImpl extends Person implements RemotePerson {
    private final int port;
    private final Map<String, RemoteAccountImpl> accounts = new ConcurrentHashMap<>();

    public RemotePersonImpl(final String name, final String surname, final int passportNumber, final int port) {
        super(name, surname, passportNumber);
        this.port = port;
    }

    @Override
    public Map<String, Account> getAccounts() {
        return new HashMap<>(accounts);
    }

    @Override
    protected Account createAccount(final String id) throws RemoteException {
        final RemoteAccountImpl account = new RemoteAccountImpl(getFullAccountId(id));
        if (accounts.putIfAbsent(id, account) == null) {
            System.out.println("Creating account " + getFullAccountId(id));
            UnicastRemoteObject.exportObject(account, port);
        }
        return accounts.get(id);
    }
}
