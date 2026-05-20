package info.kgeorgiy.ja.tregubovich.bank.account;

import java.rmi.*;

public interface RemoteAccount extends Remote {
    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money in the account. */
    int getBalance() throws RemoteException;

    /** Sets amount of money in the account. */
    void setBalance(int amount) throws NegativeBalanceException, RemoteException;
}
