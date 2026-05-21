package info.kgeorgiy.ja.tregubovich.bank.account;

import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount {
    public LocalAccount(final String id) {
        super(id);
    }

    public LocalAccount(final Account account) throws RemoteException {
        super(account.getID(), account.getBalance());
    }
}
