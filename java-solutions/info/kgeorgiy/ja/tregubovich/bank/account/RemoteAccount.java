package info.kgeorgiy.ja.tregubovich.bank.account;

import java.rmi.Remote;

public class RemoteAccount extends AbstractAccount implements Remote {
    public RemoteAccount(final String id) {
        super(id);
    }
}
