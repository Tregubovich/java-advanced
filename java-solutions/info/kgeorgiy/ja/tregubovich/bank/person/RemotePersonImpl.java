package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.RemoteAccountImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePersonImpl extends AbstractPerson implements RemotePerson {
    private final int port;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public RemotePersonImpl(final String name, final String surname, final int passportNumber, final int port) {
        super(name, surname, passportNumber);
        this.port = port;
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }

    @Override
    protected Account createAccount(final String id) throws RemoteException {
        if (accounts.containsKey(id)) {
            return accounts.get(id);
        }
        System.out.println("Creating account " + getFullAccountID(id));
        final RemoteAccountImpl account = new RemoteAccountImpl(getFullAccountID(id));
        accounts.put(id, account);
        UnicastRemoteObject.exportObject(account, port);
        return accounts.get(id);
    }
}
